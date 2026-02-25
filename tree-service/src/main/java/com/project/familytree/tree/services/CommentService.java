package com.project.familytree.tree.services;

import com.project.familytree.auth.models.User;
import com.project.familytree.auth.services.UserService;
import com.project.familytree.tree.dto.CommentDTO;
import com.project.familytree.tree.impls.NotificationType;
import com.project.familytree.tree.impls.TreeRole;
import com.project.familytree.tree.models.Comment;
import com.project.familytree.tree.models.Person;
import com.project.familytree.tree.repositories.CommentRepository;
import com.project.familytree.tree.repositories.PersonRepository;
import com.project.familytree.tree.repositories.TreeMembershipRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Сервис управления комментариями к персонам
 */
@Service
public class CommentService {

    private static final Logger log = LoggerFactory.getLogger(CommentService.class);

    private final CommentRepository commentRepository;
    private final PersonRepository personRepository;
    private final TreeMembershipRepository membershipRepository;
    private final UserService userService;
    private final TreeService treeService;
    private final NotificationService notificationService;

    public CommentService(CommentRepository commentRepository,
                          PersonRepository personRepository,
                          TreeMembershipRepository membershipRepository,
                          UserService userService,
                          TreeService treeService,
                          NotificationService notificationService) {
        this.commentRepository = commentRepository;
        this.personRepository = personRepository;
        this.membershipRepository = membershipRepository;
        this.userService = userService;
        this.treeService = treeService;
        this.notificationService = notificationService;
    }

    /**
     * Получить все комментарии к персоне в виде дерева (вложенная структура)
     */
    public List<CommentDTO> getComments(Long treeId, Long personId, Long userId) throws AccessDeniedException {
        if (!treeService.canView(treeId, userId)) {
            throw new AccessDeniedException("Нет прав на просмотр дерева");
        }

        // Загружаем все комментарии и строим дерево в памяти
        List<Comment> allComments = commentRepository.findByPersonId(personId);
        List<CommentDTO> allDTOs = allComments.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        // Группируем ответы по parentCommentId
        Map<Long, List<CommentDTO>> repliesByParent = allDTOs.stream()
                .filter(c -> c.getParentCommentId() != null)
                .collect(Collectors.groupingBy(CommentDTO::getParentCommentId));

        // Устанавливаем ответы для каждого комментария
        allDTOs.forEach(dto -> dto.setReplies(
                repliesByParent.getOrDefault(dto.getId(), new ArrayList<>())
        ));

        // Возвращаем только комментарии верхнего уровня
        return allDTOs.stream()
                .filter(c -> c.getParentCommentId() == null)
                .collect(Collectors.toList());
    }

    /**
     * Добавить комментарий к персоне
     */
    @Transactional
    public CommentDTO addComment(Long treeId, Long personId, String content,
                                  Long parentCommentId, Long userId) throws AccessDeniedException {
        if (!treeService.canView(treeId, userId)) {
            throw new AccessDeniedException("Нет прав на просмотр дерева");
        }

        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new RuntimeException("Персона не найдена"));

        if (!person.getTree().getId().equals(treeId)) {
            throw new AccessDeniedException("Персона не принадлежит этому дереву");
        }

        User author = userService.findById(userId);

        Comment parentComment = null;
        if (parentCommentId != null) {
            parentComment = commentRepository.findById(parentCommentId)
                    .orElseThrow(() -> new RuntimeException("Родительский комментарий не найден"));
        }

        Comment comment = new Comment(person, author, parentComment, content);
        comment = commentRepository.save(comment);
        log.info("User {} added comment {} to person {} in tree {}", userId, comment.getId(), personId, treeId);

        // Уведомляем владельцев и редакторов дерева о новом комментарии
        notifyTreeEditors(treeId, userId, personId, person.getFullName());

        return convertToDTO(comment);
    }

    /**
     * Редактировать комментарий (только автор)
     */
    @Transactional
    public CommentDTO editComment(Long commentId, String newContent, Long userId) throws AccessDeniedException {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Комментарий не найден"));

        if (!comment.getAuthor().getId().equals(userId)) {
            throw new AccessDeniedException("Только автор может редактировать комментарий");
        }

        if (comment.isDeleted()) {
            throw new RuntimeException("Нельзя редактировать удалённый комментарий");
        }

        comment.setContent(newContent);
        comment = commentRepository.save(comment);
        log.info("User {} edited comment {}", userId, commentId);
        return convertToDTO(comment);
    }

    /**
     * Мягкое удаление комментария (автор или OWNER/EDITOR дерева)
     */
    @Transactional
    public void deleteComment(Long treeId, Long commentId, Long userId) throws AccessDeniedException {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Комментарий не найден"));

        boolean isAuthor = comment.getAuthor().getId().equals(userId);
        boolean canModerate = treeService.canEdit(treeId, userId);

        if (!isAuthor && !canModerate) {
            throw new AccessDeniedException("Нет прав на удаление комментария");
        }

        comment.setDeleted(true);
        comment.setContent("[Комментарий удалён]");
        commentRepository.save(comment);
        log.info("User {} soft-deleted comment {}", userId, commentId);
    }

    // ─── Вспомогательные методы ───────────────────────────────────────────────────

    /**
     * Уведомить OWNER и EDITOR дерева о новом комментарии (кроме самого автора)
     */
    private void notifyTreeEditors(Long treeId, Long authorId, Long personId, String personName) {
        List<Long> recipientIds = membershipRepository.findByTreeId(treeId).stream()
                .filter(m -> m.getRole().hasPermission(TreeRole.EDITOR))
                .map(m -> m.getUser().getId())
                .filter(id -> !id.equals(authorId))
                .collect(Collectors.toList());

        if (!recipientIds.isEmpty()) {
            String content = "Новый комментарий к персоне: " + personName;
            String link = "/trees/" + treeId + "/persons/" + personId;
            notificationService.createNotificationsForUsers(
                    recipientIds, NotificationType.COMMENT_ADDED, content, link);
        }
    }

    // ─── DTO conversion ──────────────────────────────────────────────────────────

    public CommentDTO convertToDTO(Comment comment) {
        String authorName = comment.getAuthor().getFirstName() + " " + comment.getAuthor().getLastName();
        Long parentId = comment.getParentComment() != null ? comment.getParentComment().getId() : null;

        return new CommentDTO(
                comment.getId(),
                comment.getPerson().getId(),
                comment.getAuthor().getId(),
                authorName,
                parentId,
                comment.isDeleted() ? "[Комментарий удалён]" : comment.getContent(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                comment.isDeleted()
        );
    }
}
