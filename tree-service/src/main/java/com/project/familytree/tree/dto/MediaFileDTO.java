package com.project.familytree.tree.dto;

import com.project.familytree.tree.impls.MediaFileType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Медиафайл персоны")
public class MediaFileDTO {

    @Schema(description = "ID файла")
    private Long id;

    @Schema(description = "ID персоны (может быть null, если файл привязан только к дереву)")
    private Long personId;

    @Schema(description = "ID дерева")
    private Long treeId;

    @Schema(description = "Оригинальное имя файла")
    private String fileName;

    @Schema(description = "Тип файла: IMAGE, DOCUMENT, VIDEO, AUDIO")
    private MediaFileType fileType;

    @Schema(description = "Размер файла в байтах")
    private Long fileSize;

    @Schema(description = "Описание файла")
    private String description;

    @Schema(description = "Дата и время загрузки")
    private Instant uploadedAt;

    @Schema(description = "ID пользователя, загрузившего файл")
    private Long uploadedById;

    @Schema(description = "Presigned URL для скачивания файла (временный, действует 60 минут)")
    private String url;

    public MediaFileDTO() {
    }

    public MediaFileDTO(Long id, Long personId, Long treeId, String fileName,
                        MediaFileType fileType, Long fileSize, String description,
                        Instant uploadedAt, Long uploadedById, String url) {
        this.id = id;
        this.personId = personId;
        this.treeId = treeId;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.description = description;
        this.uploadedAt = uploadedAt;
        this.uploadedById = uploadedById;
        this.url = url;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPersonId() { return personId; }
    public void setPersonId(Long personId) { this.personId = personId; }

    public Long getTreeId() { return treeId; }
    public void setTreeId(Long treeId) { this.treeId = treeId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public MediaFileType getFileType() { return fileType; }
    public void setFileType(MediaFileType fileType) { this.fileType = fileType; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Instant getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Instant uploadedAt) { this.uploadedAt = uploadedAt; }

    public Long getUploadedById() { return uploadedById; }
    public void setUploadedById(Long uploadedById) { this.uploadedById = uploadedById; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
