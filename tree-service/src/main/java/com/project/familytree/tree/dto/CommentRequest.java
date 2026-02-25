package com.project.familytree.tree.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Запрос на создание или редактирование комментария
 */
public class CommentRequest {

    @NotBlank(message = "Текст комментария не может быть пустым")
    @Size(max = 2000, message = "Комментарий не может превышать 2000 символов")
    @Schema(description = "Текст комментария", example = "Очень интересная биография!")
    private String content;

    @Schema(description = "ID родительского комментария (для ответа на комментарий)", nullable = true)
    private Long parentCommentId;

    public CommentRequest() {
    }

    public CommentRequest(String content, Long parentCommentId) {
        this.content = content;
        this.parentCommentId = parentCommentId;
    }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Long getParentCommentId() { return parentCommentId; }
    public void setParentCommentId(Long parentCommentId) { this.parentCommentId = parentCommentId; }
}
