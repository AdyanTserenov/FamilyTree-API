package com.project.familytree.tree.controllers;

import com.project.familytree.auth.dto.CustomApiResponse;
import com.project.familytree.auth.services.UserService;
import com.project.familytree.tree.dto.CommentDTO;
import com.project.familytree.tree.dto.CommentRequest;
import com.project.familytree.tree.dto.PagedCommentsResponse;
import com.project.familytree.tree.services.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.List;

@RestController
@RequestMapping("/trees/{treeId}/persons/{personId}/comments")
@Tag(name = "Comment Controller", description = "API для управления комментариями к персонам")
public class CommentController {

    private static final Logger log = LoggerFactory.getLogger(CommentController.class);

    private final CommentService commentService;
    private final UserService userService;

    public CommentController(CommentService commentService, UserService userService) {
        this.commentService = commentService;
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Получить комментарии к персоне",
               description = "Возвращает комментарии с пагинацией (page/size). " +
                             "Если page и size не переданы — возвращает все комментарии. " +
                             "Требует роль VIEWER или выше.")
    public ResponseEntity<?> getComments(
            @PathVariable Long treeId,
            @PathVariable Long personId,
            @RequestParam(required = false) Integer page,
            @RequestParam(defaultValue = "10") int size) throws AccessDeniedException {

        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        log.info("Getting comments for person {} in tree {} by user {} (page={}, size={})",
                personId, treeId, userId, page, size);

        if (page != null) {
            PagedCommentsResponse paged = commentService.getCommentsPaged(treeId, personId, userId, page, size);
            return ResponseEntity.ok(CustomApiResponse.successData(paged));
        }

        List<CommentDTO> comments = commentService.getComments(treeId, personId, userId);
        return ResponseEntity.ok(CustomApiResponse.successData(comments));
    }

    @PostMapping
    @Operation(summary = "Добавить комментарий",
               description = "Создаёт новый комментарий к персоне. Поддерживает вложенность через parentCommentId. " +
                             "Требует роль VIEWER или выше.")
    public ResponseEntity<CustomApiResponse<CommentDTO>> addComment(
            @PathVariable Long treeId,
            @PathVariable Long personId,
            @Valid @RequestBody CommentRequest request) throws AccessDeniedException {

        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        log.info("User {} adding comment to person {} in tree {}", userId, personId, treeId);

        CommentDTO comment = commentService.addComment(
                treeId, personId, request.getContent(), request.getParentCommentId(), userId);
        return ResponseEntity.ok(CustomApiResponse.successData(comment));
    }

    @PutMapping("/{commentId}")
    @Operation(summary = "Редактировать комментарий",
               description = "Изменяет текст комментария. Доступно только автору комментария.")
    public ResponseEntity<CustomApiResponse<CommentDTO>> editComment(
            @PathVariable Long treeId,
            @PathVariable Long personId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentRequest request) throws AccessDeniedException {

        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        log.info("User {} editing comment {} for person {} in tree {}", userId, commentId, personId, treeId);

        CommentDTO updated = commentService.editComment(commentId, request.getContent(), userId);
        return ResponseEntity.ok(CustomApiResponse.successData(updated));
    }

    @DeleteMapping("/{commentId}")
    @Operation(summary = "Удалить комментарий",
               description = "Мягкое удаление комментария (контент скрывается). " +
                             "Доступно автору или OWNER/EDITOR дерева.")
    public ResponseEntity<CustomApiResponse<String>> deleteComment(
            @PathVariable Long treeId,
            @PathVariable Long personId,
            @PathVariable Long commentId) throws AccessDeniedException {

        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        log.info("User {} deleting comment {} for person {} in tree {}", userId, commentId, personId, treeId);

        commentService.deleteComment(treeId, commentId, userId);
        return ResponseEntity.ok(CustomApiResponse.successMessage("Комментарий удалён"));
    }
}
