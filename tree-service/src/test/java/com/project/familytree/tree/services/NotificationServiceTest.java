package com.project.familytree.tree.services;

import com.project.familytree.auth.models.User;
import com.project.familytree.auth.services.UserService;
import com.project.familytree.tree.dto.NotificationDTO;
import com.project.familytree.tree.impls.NotificationType;
import com.project.familytree.tree.models.Notification;
import com.project.familytree.tree.repositories.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserService userService;

    @InjectMocks
    private NotificationService notificationService;

    private User user;
    private Notification notification;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setFirstName("Иван");
        user.setLastName("Иванов");
        user.setEmail("ivan@test.com");

        notification = new Notification(user, NotificationType.COMMENT_ADDED,
                "Новый комментарий", "/trees/10/persons/20");
        notification.setId(50L);
        notification.setCreatedAt(Instant.now());
    }

    // ─── createNotification ───────────────────────────────────────────────────────

    @Test
    void createNotification_savesAndReturnsDTO() {
        when(userService.findById(1L)).thenReturn(user);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(51L);
            n.setCreatedAt(Instant.now());
            return n;
        });

        NotificationDTO result = notificationService.createNotification(
                1L, NotificationType.COMMENT_ADDED, "Тест", "/link");

        assertThat(result.getId()).isEqualTo(51L);
        assertThat(result.getUserId()).isEqualTo(1L);
        assertThat(result.getType()).isEqualTo(NotificationType.COMMENT_ADDED);
        assertThat(result.getContent()).isEqualTo("Тест");
        assertThat(result.getLink()).isEqualTo("/link");
        assertThat(result.isRead()).isFalse();
        verify(notificationRepository).save(any(Notification.class));
    }

    // ─── createNotificationsForUsers ─────────────────────────────────────────────

    @Test
    void createNotificationsForUsers_createsForEachUser() {
        User user2 = new User();
        user2.setId(2L);
        user2.setFirstName("Пётр");
        user2.setLastName("Петров");

        when(userService.findById(1L)).thenReturn(user);
        when(userService.findById(2L)).thenReturn(user2);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(100L);
            n.setCreatedAt(Instant.now());
            return n;
        });

        notificationService.createNotificationsForUsers(
                List.of(1L, 2L), NotificationType.PERSON_ADDED, "Добавлена персона", "/link");

        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void createNotificationsForUsers_whenOneUserFails_continuesForOthers() {
        User user2 = new User();
        user2.setId(2L);
        user2.setFirstName("Пётр");
        user2.setLastName("Петров");

        when(userService.findById(1L)).thenThrow(new RuntimeException("Пользователь не найден"));
        when(userService.findById(2L)).thenReturn(user2);
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> {
            Notification n = inv.getArgument(0);
            n.setId(100L);
            n.setCreatedAt(Instant.now());
            return n;
        });

        // Should not throw
        assertThatCode(() -> notificationService.createNotificationsForUsers(
                List.of(1L, 2L), NotificationType.COMMENT_ADDED, "text", "/link"))
                .doesNotThrowAnyException();

        // Only user2 gets notification
        verify(notificationRepository, times(1)).save(any(Notification.class));
    }

    @Test
    void createNotificationsForUsers_emptyList_doesNothing() {
        notificationService.createNotificationsForUsers(
                List.of(), NotificationType.COMMENT_ADDED, "text", "/link");

        verifyNoInteractions(notificationRepository);
    }

    // ─── getUserNotifications ─────────────────────────────────────────────────────

    @Test
    void getUserNotifications_returnsAllForUser() {
        Notification n2 = new Notification(user, NotificationType.PERSON_ADDED,
                "Добавлена персона", "/trees/10");
        n2.setId(51L);
        n2.setCreatedAt(Instant.now());

        when(notificationRepository.findByUserIdOrderByReadAscCreatedAtDesc(1L))
                .thenReturn(List.of(notification, n2));

        List<NotificationDTO> result = notificationService.getUserNotifications(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(50L);
        assertThat(result.get(1).getId()).isEqualTo(51L);
    }

    @Test
    void getUserNotifications_emptyList_returnsEmpty() {
        when(notificationRepository.findByUserIdOrderByReadAscCreatedAtDesc(1L))
                .thenReturn(List.of());

        List<NotificationDTO> result = notificationService.getUserNotifications(1L);

        assertThat(result).isEmpty();
    }

    // ─── countUnread ──────────────────────────────────────────────────────────────

    @Test
    void countUnread_returnsRepositoryCount() {
        when(notificationRepository.countUnreadByUserId(1L)).thenReturn(3L);

        long count = notificationService.countUnread(1L);

        assertThat(count).isEqualTo(3L);
    }

    // ─── markAsRead ───────────────────────────────────────────────────────────────

    @Test
    void markAsRead_byOwner_setsReadTrue() {
        when(notificationRepository.findById(50L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationDTO result = notificationService.markAsRead(50L, 1L);

        assertThat(result.isRead()).isTrue();
        verify(notificationRepository).save(notification);
    }

    @Test
    void markAsRead_byOtherUser_throwsRuntimeException() {
        when(notificationRepository.findById(50L)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.markAsRead(50L, 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("доступ");
    }

    @Test
    void markAsRead_notFound_throwsRuntimeException() {
        when(notificationRepository.findById(50L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(50L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("не найдено");
    }

    // ─── markAllAsRead ────────────────────────────────────────────────────────────

    @Test
    void markAllAsRead_callsRepository() {
        notificationService.markAllAsRead(1L);

        verify(notificationRepository).markAllAsReadByUserId(1L);
    }

    // ─── deleteNotification ───────────────────────────────────────────────────────

    @Test
    void deleteNotification_byOwner_deletesFromRepository() {
        when(notificationRepository.findById(50L)).thenReturn(Optional.of(notification));

        notificationService.deleteNotification(50L, 1L);

        verify(notificationRepository).delete(notification);
    }

    @Test
    void deleteNotification_byOtherUser_throwsRuntimeException() {
        when(notificationRepository.findById(50L)).thenReturn(Optional.of(notification));

        assertThatThrownBy(() -> notificationService.deleteNotification(50L, 99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("доступ");
    }

    @Test
    void deleteNotification_notFound_throwsRuntimeException() {
        when(notificationRepository.findById(50L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.deleteNotification(50L, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("не найдено");
    }

    // ─── convertToDTO ─────────────────────────────────────────────────────────────

    @Test
    void convertToDTO_mapsAllFields() {
        NotificationDTO dto = notificationService.convertToDTO(notification);

        assertThat(dto.getId()).isEqualTo(50L);
        assertThat(dto.getUserId()).isEqualTo(1L);
        assertThat(dto.getType()).isEqualTo(NotificationType.COMMENT_ADDED);
        assertThat(dto.getContent()).isEqualTo("Новый комментарий");
        assertThat(dto.getLink()).isEqualTo("/trees/10/persons/20");
        assertThat(dto.isRead()).isFalse();
        assertThat(dto.getCreatedAt()).isNotNull();
    }
}
