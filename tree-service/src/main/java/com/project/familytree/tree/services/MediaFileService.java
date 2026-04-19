package com.project.familytree.tree.services;

import com.project.familytree.auth.models.User;
import com.project.familytree.auth.services.UserService;
import com.project.familytree.tree.dto.MediaFileDTO;
import com.project.familytree.tree.exceptions.BusinessException;
import com.project.familytree.tree.exceptions.ResourceNotFoundException;
import com.project.familytree.tree.impls.MediaFileType;
import com.project.familytree.tree.models.MediaFile;
import com.project.familytree.tree.models.Person;
import com.project.familytree.tree.models.Tree;
import com.project.familytree.tree.repositories.MediaFileRepository;
import com.project.familytree.tree.repositories.PersonRepository;
import com.project.familytree.tree.repositories.TreeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Сервис управления медиафайлами.
 * Файлы хранятся в Yandex Object Storage (S3-совместимый).
 * <p>
 * Структура ключей в бакете:
 *   trees/{treeId}/media/{uuid}.ext   — медиафайлы персон
 *   trees/{treeId}/avatars/{uuid}.ext — аватары персон
 */
@Service
public class MediaFileService {

    private static final Logger log = LoggerFactory.getLogger(MediaFileService.class);
    private static final int MAX_MEDIA_FILES = 10;

    /** Whitelist of allowed file extensions. Executable and script types are blocked. */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".svg",
            ".pdf",
            ".mp4", ".mov", ".avi", ".mkv", ".webm",
            ".mp3", ".wav", ".ogg", ".flac",
            ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
            ".txt", ".rtf"
    );

    private final MediaFileRepository mediaFileRepository;
    private final PersonRepository personRepository;
    private final TreeRepository treeRepository;
    private final UserService userService;
    private final TreeService treeService;
    private final S3Service s3Service;

    public MediaFileService(MediaFileRepository mediaFileRepository,
                            PersonRepository personRepository,
                            TreeRepository treeRepository,
                            UserService userService,
                            TreeService treeService,
                            S3Service s3Service) {
        this.mediaFileRepository = mediaFileRepository;
        this.personRepository = personRepository;
        this.treeRepository = treeRepository;
        this.userService = userService;
        this.treeService = treeService;
        this.s3Service = s3Service;
    }

    @Transactional
    public MediaFileDTO uploadFile(Long treeId, Long personId, MultipartFile file,
                                   MediaFileType fileType, String description,
                                   Long userId) throws AccessDeniedException, IOException {
        if (!treeService.canEdit(treeId, userId)) {
            throw new AccessDeniedException("Нет прав на редактирование дерева");
        }

        Tree tree = treeRepository.findById(treeId)
                .orElseThrow(() -> new ResourceNotFoundException("Дерево не найдено"));

        Person person = null;
        if (personId != null) {
            person = personRepository.findById(personId)
                    .orElseThrow(() -> new ResourceNotFoundException("Персона не найдена"));
            if (!person.getTree().getId().equals(treeId)) {
                throw new AccessDeniedException("Персона не принадлежит этому дереву");
            }
            long existingCount = mediaFileRepository.countByPersonId(personId);
            if (existingCount >= MAX_MEDIA_FILES) {
                throw new BusinessException("Достигнут лимит медиафайлов (" + MAX_MEDIA_FILES + ") для данной персоны");
            }
        }

        User uploader = userService.findById(userId);

        // Генерируем S3-ключ: trees/{treeId}/media/{uuid}.ext
        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);

        // Validate extension against whitelist
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("Недопустимый тип файла: " + extension +
                    ". Разрешены: изображения, PDF, видео, аудио, документы Office, текстовые файлы.");
        }

        String s3Key = "trees/" + treeId + "/media/" + UUID.randomUUID() + extension;

        // Загружаем в S3
        s3Service.upload(s3Key, file.getInputStream(),
                resolveContentType(file.getContentType(), extension),
                file.getSize());
        log.info("Uploaded media file to S3: {} (tree={}, person={})", s3Key, treeId, personId);

        // Сохраняем метаданные в БД (filePath = s3Key)
        MediaFile mediaFile = new MediaFile(
                person,
                tree,
                originalFilename != null ? originalFilename : s3Key,
                s3Key,
                fileType,
                file.getSize(),
                description,
                uploader
        );

        mediaFile = mediaFileRepository.save(mediaFile);
        return convertToDTO(mediaFile);
    }

    public List<MediaFileDTO> getPersonMedia(Long treeId, Long personId, Long userId) throws AccessDeniedException {
        if (!treeService.canView(treeId, userId)) {
            throw new AccessDeniedException("Нет прав на просмотр дерева");
        }

        return mediaFileRepository.findByTreeIdAndPersonId(treeId, personId)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    public List<MediaFileDTO> getTreeMedia(Long treeId, Long userId) throws AccessDeniedException {
        if (!treeService.canView(treeId, userId)) {
            throw new AccessDeniedException("Нет прав на просмотр дерева");
        }

        return mediaFileRepository.findByTreeId(treeId)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    /**
     * Результат скачивания файла: ресурс + оригинальное имя файла.
     */
    public record DownloadResult(Resource resource, String fileName) {}

    /**
     * Скачать файл из S3 и вернуть как Resource (проксирование через бэкенд).
     * Используется для файлов, которые не должны быть публично доступны.
     */
    public DownloadResult downloadFile(Long treeId, Long personId, Long fileId, Long userId)
            throws AccessDeniedException {
        if (!treeService.canView(treeId, userId)) {
            throw new AccessDeniedException("Нет прав на просмотр дерева");
        }

        MediaFile mediaFile = mediaFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("Файл не найден"));

        if (!mediaFile.getTree().getId().equals(treeId)) {
            throw new AccessDeniedException("Файл не принадлежит этому дереву");
        }

        if (personId != null && (mediaFile.getPerson() == null || !mediaFile.getPerson().getId().equals(personId))) {
            throw new AccessDeniedException("Файл не принадлежит этой персоне");
        }

        ResponseInputStream<GetObjectResponse> s3Stream = s3Service.download(mediaFile.getFilePath());
        String fileName = mediaFile.getFileName() != null ? mediaFile.getFileName() : "file";
        return new DownloadResult(new InputStreamResource(s3Stream), fileName);
    }

    @Transactional
    public void deleteFile(Long treeId, Long personId, Long fileId, Long userId) throws AccessDeniedException {
        if (!treeService.canEdit(treeId, userId)) {
            throw new AccessDeniedException("Нет прав на редактирование дерева");
        }

        MediaFile mediaFile = mediaFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("Файл не найден"));

        if (!mediaFile.getTree().getId().equals(treeId)) {
            throw new AccessDeniedException("Файл не принадлежит этому дереву");
        }

        if (personId != null && (mediaFile.getPerson() == null || !mediaFile.getPerson().getId().equals(personId))) {
            throw new AccessDeniedException("Файл не принадлежит этой персоне");
        }

        // Удаляем из S3
        try {
            s3Service.delete(mediaFile.getFilePath());
        } catch (Exception e) {
            log.warn("Could not delete file from S3: {}", mediaFile.getFilePath(), e);
        }

        mediaFileRepository.delete(mediaFile);
    }

    public MediaFileDTO convertToDTO(MediaFile mediaFile) {
        // Генерируем presigned URL для временного доступа
        String url = null;
        try {
            url = s3Service.generatePresignedUrl(mediaFile.getFilePath());
        } catch (Exception e) {
            log.warn("Could not generate presigned URL for {}: {}", mediaFile.getFilePath(), e.getMessage());
        }

        return new MediaFileDTO(
                mediaFile.getId(),
                mediaFile.getPerson() != null ? mediaFile.getPerson().getId() : null,
                mediaFile.getTree().getId(),
                mediaFile.getFileName(),
                mediaFile.getFileType(),
                mediaFile.getFileSize(),
                mediaFile.getDescription(),
                mediaFile.getUploadedAt(),
                mediaFile.getUploadedBy().getId(),
                url
        );
    }

    // ─── helpers ─────────────────────────────────────────────────────────────────

    private String extractExtension(String filename) {
        if (filename != null && filename.contains(".")) {
            return filename.substring(filename.lastIndexOf(".")).toLowerCase();
        }
        return "";
    }

    private String resolveContentType(String contentType, String extension) {
        if (contentType != null && !contentType.isBlank() && !contentType.equals("application/octet-stream")) {
            return contentType;
        }
        return switch (extension) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png"          -> "image/png";
            case ".gif"          -> "image/gif";
            case ".webp"         -> "image/webp";
            case ".pdf"          -> "application/pdf";
            case ".mp4"          -> "video/mp4";
            case ".mp3"          -> "audio/mpeg";
            case ".doc"          -> "application/msword";
            case ".docx"         -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default              -> "application/octet-stream";
        };
    }
}
