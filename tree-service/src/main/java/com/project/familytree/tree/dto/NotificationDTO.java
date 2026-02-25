package com.project.familytree.tree.dto;

import com.project.familytree.tree.impls.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * DTO для передачи данных уведомления клиенту
 */
public class NotificationDTO {

    @Schema(description = "ID уведомления")
    private Long id;

    @Schema(description = "ID пользователя-получателя")
    private Long userId;

    @Schema(description = "Тип уведомления")
    private NotificationType type;

    @Schema(description = "Текст уведомления")
    private String content;

    @Schema(description = "Ссылка на связанный ресурс")
    private String link;

    @Schema(description = "Прочитано ли уведомление")
    private boolean read;

    @Schema(description = "Дата создания")
    private Instant createdAt;

    public NotificationDTO() {
    }

    public NotificationDTO(Long id, Long userId, NotificationType type,
                           String content, String link, boolean read, Instant createdAt) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.content = content;
        this.link = link;
        this.read = read;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
