package com.project.familytree.tree.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

/**
 * DTO для передачи данных комментария клиенту
 */
public class CommentDTO {

    @Schema(description = "ID комментария")
    private Long id;

    @Schema(description = "ID персоны, к которой относится комментарий")
    private Long personId;

    @Schema(description = "ID автора комментария")
    private Long authorId;

    @Schema(description = "Полное имя автора")
    private String authorName;

    @Schema(description = "ID родительского комментария (null — комментарий верхнего уровня)")
    private Long parentCommentId;

    @Schema(description = "Текст комментария (null если удалён)")
    private String content;

    @Schema(description = "Дата создания")
    private Instant createdAt;

    @Schema(description = "Дата последнего редактирования")
    private Instant updatedAt;

    @Schema(description = "Флаг мягкого удаления")
    private boolean deleted;

    @Schema(description = "Ответы на этот комментарий")
    private List<CommentDTO> replies;

    public CommentDTO() {
    }

    public CommentDTO(Long id, Long personId, Long authorId, String authorName,
                      Long parentCommentId, String content,
                      Instant createdAt, Instant updatedAt, boolean deleted) {
        this.id = id;
        this.personId = personId;
        this.authorId = authorId;
        this.authorName = authorName;
        this.parentCommentId = parentCommentId;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deleted = deleted;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPersonId() { return personId; }
    public void setPersonId(Long personId) { this.personId = personId; }

    public Long getAuthorId() { return authorId; }
    public void setAuthorId(Long authorId) { this.authorId = authorId; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public Long getParentCommentId() { return parentCommentId; }
    public void setParentCommentId(Long parentCommentId) { this.parentCommentId = parentCommentId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }

    public List<CommentDTO> getReplies() { return replies; }
    public void setReplies(List<CommentDTO> replies) { this.replies = replies; }
}
