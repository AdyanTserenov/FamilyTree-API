package com.project.familytree.tree.services;

import com.project.familytree.auth.models.User;
import com.project.familytree.auth.services.UserService;
import com.project.familytree.tree.dto.CommentDTO;
import com.project.familytree.tree.impls.Gender;
import com.project.familytree.tree.impls.NotificationType;
import com.project.familytree.tree.impls.TreeRole;
import com.project.familytree.tree.models.Comment;
import com.project.familytree.tree.models.Person;
import com.project.familytree.tree.models.Tree;
import com.project.familytree.tree.models.TreeMembership;
import com.project.familytree.tree.repositories.CommentRepository;
import com.project.familytree.tree.repositories.PersonRepository;
import com.project.familytree.tree.repositories.TreeMembershipRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.AccessDeniedException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock private CommentRepository commentRepository;
    @Mock private PersonRepository personRepository;
    @Mock private TreeMembershipRepository membershipRepository;
    @Mock private UserService userService;
    @Mock private TreeService treeService;
    @Mock private NotificationService notificationService;

    @InjectMocks
    private CommentService commentService;

    private User author;
    private Tree tree;
    private Person person;
    private Comment comment;

    @BeforeEach
    void setUp() {
        author = new User();
        author.setId(1L);
        author.setFirstName("Иван");
        author.setLastName("Иванов");
        author.setEmail("ivan@test.com");

        tree = new Tree();
        tree.setId(10L);
        tree.setName("Тестовое дерево");

        person = new Person(tree, "Пётр", "Петров", null, Gender.MALE);
        person.setId(20L);

        comment = new Comment(person, author, null, "Тестовый комментарий");
        comment.setId(100L);
        comment.setCreatedAt(Instant.now());
        comment.setUpdatedAt(Instant.now());
    }

    // ─── getComments ─────────────────────────────────────────────────────────────

    @Test
    void getComments_whenCanView_returnsTopLevelTree() throws AccessDeniedException {
        when(treeService.canView(10L, 1L)).thenReturn(true);
        when(commentRepository.findByPersonId(20L)).thenReturn(List.of(comment));

        List<CommentDTO> result = commentService.getComments(10L, 20L, 1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("Тестовый комментарий");
        assertThat(result.get(0).getParentCommentId()).isNull();
    }

    @Test
    void getComments_whenNoAccess_throwsAccessDeniedException() {
        when(treeService.canView(10L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> commentService.getComments(10L, 20L, 1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getComments_buildsNestedReplies() throws AccessDeniedException {
        Comment reply = new Comment(person, author, comment, "Ответ");
        reply.setId(101L);
        reply.setCreatedAt(Instant.now());
        reply.setUpdatedAt(Instant.now());

        when(treeService.canView(10L, 1L)).thenReturn(true);
        when(commentRepository.findByPersonId(20L)).thenReturn(List.of(comment, reply));

        List<CommentDTO> result = commentService.getComments(10L, 20L, 1L);

        // Only top-level returned
        assertThat(result).hasSize(1);
        // Reply is nested
        assertThat(result.get(0).getReplies()).hasSize(1);
        assertThat(result.get(0).getReplies().get(0).getContent()).isEqualTo("Ответ");
    }

    @Test
    void getComments_emptyList_returnsEmpty() throws AccessDeniedException {
        when(treeService.canView(10L, 1L)).thenReturn(true);
        when(commentRepository.findByPersonId(20L)).thenReturn(List.of());

        List<CommentDTO> result = commentService.getComments(10L, 20L, 1L);

        assertThat(result).isEmpty();
    }

    // ─── addComment ──────────────────────────────────────────────────────────────

    @Test
    void addComment_whenCanView_savesAndReturnsDTO() throws AccessDeniedException {
        when(treeService.canView(10L, 1L)).thenReturn(true);
        when(personRepository.findById(20L)).thenReturn(Optional.of(person));
        when(userService.findById(1L)).thenReturn(author);
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(200L);
            c.setCreatedAt(Instant.now());
            c.setUpdatedAt(Instant.now());
            return c;
        });
        when(membershipRepository.findByTreeId(10L)).thenReturn(List.of());

        CommentDTO result = commentService.addComment(10L, 20L, "Новый комментарий", null, 1L);

        assertThat(result.getId()).isEqualTo(200L);
        assertThat(result.getContent()).isEqualTo("Новый комментарий");
        assertThat(result.getAuthorId()).isEqualTo(1L);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void addComment_withParentComment_setsParent() throws AccessDeniedException {
        when(treeService.canView(10L, 1L)).thenReturn(true);
        when(personRepository.findById(20L)).thenReturn(Optional.of(person));
        when(userService.findById(1L)).thenReturn(author);
        when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(201L);
            c.setCreatedAt(Instant.now());
            c.setUpdatedAt(Instant.now());
            return c;
        });
        when(membershipRepository.findByTreeId(10L)).thenReturn(List.of());

        CommentDTO result = commentService.addComment(10L, 20L, "Ответ", 100L, 1L);

        assertThat(result.getParentCommentId()).isEqualTo(100L);
    }

    @Test
    void addComment_whenNoAccess_throwsAccessDeniedException() {
        when(treeService.canView(10L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> commentService.addComment(10L, 20L, "text", null, 1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void addComment_personNotFound_throwsRuntimeException() {
        when(treeService.canView(10L, 1L)).thenReturn(true);
        when(personRepository.findById(20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.addComment(10L, 20L, "text", null, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("не найдена");
    }

    @Test
    void addComment_notifiesEditors() throws AccessDeniedException {
        User editor = new User();
        editor.setId(2L);
        editor.setFirstName("Редактор");
        editor.setLastName("Тест");

        TreeMembership editorMembership = new TreeMembership();
        editorMembership.setUser(editor);
        editorMembership.setRole(TreeRole.EDITOR);

        when(treeService.canView(10L, 1L)).thenReturn(true);
        when(personRepository.findById(20L)).thenReturn(Optional.of(person));
        when(userService.findById(1L)).thenReturn(author);
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(202L);
            c.setCreatedAt(Instant.now());
            c.setUpdatedAt(Instant.now());
            return c;
        });
        when(membershipRepository.findByTreeId(10L)).thenReturn(List.of(editorMembership));

        commentService.addComment(10L, 20L, "text", null, 1L);

        verify(notificationService).createNotificationsForUsers(
                eq(List.of(2L)),
                eq(NotificationType.COMMENT_ADDED),
                anyString(),
                anyString()
        );
    }

    // ─── editComment ─────────────────────────────────────────────────────────────

    @Test
    void editComment_byAuthor_updatesContent() throws AccessDeniedException {
        when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));

        CommentDTO result = commentService.editComment(100L, "Обновлённый текст", 1L);

        assertThat(result.getContent()).isEqualTo("Обновлённый текст");
        verify(commentRepository).save(comment);
    }

    @Test
    void editComment_byNonAuthor_throwsAccessDeniedException() {
        when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.editComment(100L, "text", 99L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void editComment_deletedComment_throwsRuntimeException() {
        comment.setDeleted(true);
        when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService.editComment(100L, "text", 1L))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void editComment_notFound_throwsRuntimeException() {
        when(commentRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.editComment(100L, "text", 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("не найден");
    }

    // ─── deleteComment ────────────────────────────────────────────────────────────

    @Test
    void deleteComment_byAuthor_softDeletes() throws AccessDeniedException {
        when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));
        when(treeService.canEdit(10L, 1L)).thenReturn(false); // not editor, but is author

        commentService.deleteComment(10L, 100L, 1L);

        assertThat(comment.isDeleted()).isTrue();
        assertThat(comment.getContent()).isEqualTo("[Комментарий удалён]");
        verify(commentRepository).save(comment);
    }

    @Test
    void deleteComment_byEditor_softDeletes() throws AccessDeniedException {
        when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));
        when(treeService.canEdit(10L, 99L)).thenReturn(true); // editor, not author

        commentService.deleteComment(10L, 100L, 99L);

        assertThat(comment.isDeleted()).isTrue();
        verify(commentRepository).save(comment);
    }

    @Test
    void deleteComment_byUnauthorizedUser_throwsAccessDeniedException() {
        when(commentRepository.findById(100L)).thenReturn(Optional.of(comment));
        when(treeService.canEdit(10L, 99L)).thenReturn(false);

        assertThatThrownBy(() -> commentService.deleteComment(10L, 100L, 99L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deleteComment_notFound_throwsRuntimeException() {
        when(commentRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.deleteComment(10L, 100L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("не найден");
    }

    // ─── convertToDTO ─────────────────────────────────────────────────────────────

    @Test
    void convertToDTO_deletedComment_masksContent() {
        comment.setDeleted(true);
        comment.setContent("[Комментарий удалён]");

        CommentDTO dto = commentService.convertToDTO(comment);

        assertThat(dto.isDeleted()).isTrue();
        assertThat(dto.getContent()).isEqualTo("[Комментарий удалён]");
    }

    @Test
    void convertToDTO_normalComment_mapsAllFields() {
        CommentDTO dto = commentService.convertToDTO(comment);

        assertThat(dto.getId()).isEqualTo(100L);
        assertThat(dto.getPersonId()).isEqualTo(20L);
        assertThat(dto.getAuthorId()).isEqualTo(1L);
        assertThat(dto.getAuthorName()).isEqualTo("Иван Иванов");
        assertThat(dto.getParentCommentId()).isNull();
        assertThat(dto.isDeleted()).isFalse();
    }
}
