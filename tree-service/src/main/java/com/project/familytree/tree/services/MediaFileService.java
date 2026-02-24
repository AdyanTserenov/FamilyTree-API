package com.project.familytree.tree.services;

import com.project.familytree.auth.models.User;
import com.project.familytree.auth.services.UserService;
import com.project.familytree.tree.dto.MediaFileDTO;
import com.project.familytree.tree.impls.MediaFileType;
import com.project.familytree.tree.models.MediaFile;
import com.project.familytree.tree.models.Person;
import com.project.familytree.tree.models.Tree;
import com.project.familytree.tree.repositories.MediaFileRepository;
import com.project.familytree.tree.repositories.PersonRepository;
import com.project.familytree.tree.repositories.TreeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class MediaFileService {

    private static final Logger log = LoggerFactory.getLogger(MediaFileService.class);

    private final MediaFileRepository mediaFileRepository;
    private final PersonRepository personRepository;
    private final TreeRepository treeRepository;
    private final UserService userService;
    private final TreeService treeService;

    @Value("${media.upload.dir:./uploads}")
    private String uploadDir;

    public MediaFileService(MediaFileRepository mediaFileRepository,
                            PersonRepository personRepository,
                            TreeRepository treeRepository,
                            UserService userService,
                            TreeService treeService) {
        this.mediaFileRepository = mediaFileRepository;
        this.personRepository = personRepository;
        this.treeRepository = treeRepository;
        this.userService = userService;
        this.treeService = treeService;
    }

    @Transactional
    public MediaFileDTO uploadFile(Long treeId, Long personId, MultipartFile file,
                                   MediaFileType fileType, String description,
                                   Long userId) throws AccessDeniedException, IOException {
        if (!treeService.canEdit(treeId, userId)) {
            throw new AccessDeniedException("Нет прав на редактирование дерева");
        }

        Tree tree = treeRepository.findById(treeId)
                .orElseThrow(() -> new RuntimeException("Дерево не найдено"));

        Person person = null;
        if (personId != null) {
            person = personRepository.findById(personId)
                    .orElseThrow(() -> new RuntimeException("Персона не найдена"));
            if (!person.getTree().getId().equals(treeId)) {
                throw new AccessDeniedException("Персона не принадлежит этому дереву");
            }
        }

        User uploader = userService.findById(userId);

        // Создаём директорию для хранения файлов дерева
        Path treeUploadPath = Paths.get(uploadDir, "trees", treeId.toString());
        Files.createDirectories(treeUploadPath);

        // Генерируем уникальное имя файла, сохраняя расширение
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String storedFileName = UUID.randomUUID() + extension;
        Path targetPath = treeUploadPath.resolve(storedFileName);

        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Saved file {} to {}", originalFilename, targetPath);

        MediaFile mediaFile = new MediaFile(
                person,
                tree,
                originalFilename != null ? originalFilename : storedFileName,
                targetPath.toString(),
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

    public Resource downloadFile(Long treeId, Long personId, Long fileId, Long userId)
            throws AccessDeniedException, MalformedURLException {
        if (!treeService.canView(treeId, userId)) {
            throw new AccessDeniedException("Нет прав на просмотр дерева");
        }

        MediaFile mediaFile = mediaFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("Файл не найден"));

        if (!mediaFile.getTree().getId().equals(treeId)) {
            throw new AccessDeniedException("Файл не принадлежит этому дереву");
        }

        if (personId != null && (mediaFile.getPerson() == null || !mediaFile.getPerson().getId().equals(personId))) {
            throw new AccessDeniedException("Файл не принадлежит этой персоне");
        }

        Path filePath = Paths.get(mediaFile.getFilePath());
        Resource resource = new UrlResource(filePath.toUri());

        if (!resource.exists() || !resource.isReadable()) {
            throw new RuntimeException("Файл не найден на диске: " + mediaFile.getFilePath());
        }

        return resource;
    }

    @Transactional
    public void deleteFile(Long treeId, Long personId, Long fileId, Long userId) throws AccessDeniedException {
        if (!treeService.canEdit(treeId, userId)) {
            throw new AccessDeniedException("Нет прав на редактирование дерева");
        }

        MediaFile mediaFile = mediaFileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("Файл не найден"));

        if (!mediaFile.getTree().getId().equals(treeId)) {
            throw new AccessDeniedException("Файл не принадлежит этому дереву");
        }

        if (personId != null && (mediaFile.getPerson() == null || !mediaFile.getPerson().getId().equals(personId))) {
            throw new AccessDeniedException("Файл не принадлежит этой персоне");
        }

        // Удаляем физический файл
        try {
            Path filePath = Paths.get(mediaFile.getFilePath());
            Files.deleteIfExists(filePath);
            log.info("Deleted file from disk: {}", filePath);
        } catch (IOException e) {
            log.warn("Could not delete file from disk: {}", mediaFile.getFilePath(), e);
        }

        mediaFileRepository.delete(mediaFile);
    }

    public MediaFileDTO convertToDTO(MediaFile mediaFile) {
        return new MediaFileDTO(
                mediaFile.getId(),
                mediaFile.getPerson() != null ? mediaFile.getPerson().getId() : null,
                mediaFile.getTree().getId(),
                mediaFile.getFileName(),
                mediaFile.getFileType(),
                mediaFile.getFileSize(),
                mediaFile.getDescription(),
                mediaFile.getUploadedAt(),
                mediaFile.getUploadedBy().getId()
        );
    }
}
