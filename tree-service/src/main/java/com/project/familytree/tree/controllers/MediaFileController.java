package com.project.familytree.tree.controllers;

import com.project.familytree.auth.dto.CustomApiResponse;
import com.project.familytree.auth.services.UserService;
import com.project.familytree.tree.dto.MediaFileDTO;
import com.project.familytree.tree.impls.MediaFileType;
import com.project.familytree.tree.services.MediaFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@RequestMapping("/trees/{treeId}/persons/{personId}/media")
@Tag(name = "Media File Controller", description = "API для управления медиафайлами персон в семейном древе")
public class MediaFileController {

    private static final Logger log = LoggerFactory.getLogger(MediaFileController.class);

    private final MediaFileService mediaFileService;
    private final UserService userService;

    public MediaFileController(MediaFileService mediaFileService, UserService userService) {
        this.mediaFileService = mediaFileService;
        this.userService = userService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Загрузить медиафайл для персоны",
               description = "Загружает файл (фото, документ, видео, аудио) и привязывает его к персоне в дереве")
    public ResponseEntity<CustomApiResponse<MediaFileDTO>> uploadFile(
            @PathVariable Long treeId,
            @PathVariable Long personId,
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Тип файла: PHOTO, DOCUMENT, VIDEO, AUDIO")
            @RequestParam("fileType") MediaFileType fileType,
            @RequestParam(value = "description", required = false) String description) throws AccessDeniedException, IOException {

        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        log.info("Uploading file '{}' for person {} in tree {} by user {}", file.getOriginalFilename(), personId, treeId, userId);

        MediaFileDTO dto = mediaFileService.uploadFile(treeId, personId, file, fileType, description, userId);
        log.info("Uploaded file with id {} for person {} in tree {}", dto.getId(), personId, treeId);

        return ResponseEntity.ok(CustomApiResponse.successData(dto));
    }

    @GetMapping
    @Operation(summary = "Получить список медиафайлов персоны",
               description = "Возвращает все медиафайлы, привязанные к указанной персоне в дереве")
    public ResponseEntity<CustomApiResponse<List<MediaFileDTO>>> getPersonMedia(
            @PathVariable Long treeId,
            @PathVariable Long personId) throws AccessDeniedException {

        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        log.info("Getting media for person {} in tree {} by user {}", personId, treeId, userId);

        List<MediaFileDTO> files = mediaFileService.getPersonMedia(treeId, personId, userId);
        return ResponseEntity.ok(CustomApiResponse.successData(files));
    }

    @GetMapping("/{fileId}/download")
    @Operation(summary = "Скачать медиафайл",
               description = "Возвращает содержимое файла для скачивания")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long treeId,
            @PathVariable Long personId,
            @PathVariable Long fileId) throws AccessDeniedException, IOException {

        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        log.info("Downloading file {} for person {} in tree {} by user {}", fileId, personId, treeId, userId);

        Resource resource = mediaFileService.downloadFile(treeId, personId, fileId, userId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    @DeleteMapping("/{fileId}")
    @Operation(summary = "Удалить медиафайл",
               description = "Удаляет медиафайл персоны из дерева и с диска")
    public ResponseEntity<CustomApiResponse<String>> deleteFile(
            @PathVariable Long treeId,
            @PathVariable Long personId,
            @PathVariable Long fileId) throws AccessDeniedException {

        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        log.info("Deleting file {} for person {} in tree {} by user {}", fileId, personId, treeId, userId);

        mediaFileService.deleteFile(treeId, personId, fileId, userId);
        log.info("Deleted file {} from tree {}", fileId, treeId);

        return ResponseEntity.ok(CustomApiResponse.successMessage("Файл удалён"));
    }
}
