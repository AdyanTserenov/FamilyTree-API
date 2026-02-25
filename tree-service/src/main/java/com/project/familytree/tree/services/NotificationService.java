package com.project.familytree.tree.services;

import com.project.familytree.auth.models.User;
import com.project.familytree.auth.services.UserService;
import com.project.familytree.tree.dto.NotificationDTO;
import com.project.familytree.tree.impls.NotificationType;
import com.project.familytree.tree.models.Notification;
import com.project.familytree.tree.repositories.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Сервис управления уведомлениями пользователей
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final UserService userService;

    public NotificationService(NotificationRepository notificationRepository,
                                UserService userService) {
        this.notificationRepository = notificationRepository;
        this.userService = userService;
    }

    /**
     * Создать уведомление для пользователя
     */
    @Transactional
    public NotificationDTO createNotification(Long userId, NotificationType type,
                                               String content, String link) {
        User user = userService.findById(userId);
        Notification notification = new Notification(user, type, content, link);
        notification = notificationRepository.save(notification);
        log.info("Created notification [{}] for user {}", type, userId);
        return convertToDTO(notification);
    }

    /**
     * Создать уведомления для нескольких пользователей сразу
     */
    @Transactional
    public void createNotificationsForUsers(List<Long> userIds, NotificationType type,
                                             String content, String link) {
        for (Long userId : userIds) {
            try {
                createNotification(userId, type, content, link);
            } catch (Exception e) {
                log.warn("Failed to create notification for user {}: {}", userId, e.getMessage());
            }
        }
    }

    /**
     * Получить все уведомления текущего пользователя (непрочитанные первыми)
     */
    public List<NotificationDTO> getUserNotifications(Long userId) {
        return notificationRepository
                .findByUserIdOrderByReadAscCreatedAtDesc(userId)
                .stream()
                .map(this::convertToDTO)
                .toList();
    }

    /**
     * Количество непрочитанных уведомлений
     */
    public long countUnread(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    /**
     * Отметить одно уведомление как прочитанное
     */
    @Transactional
    public NotificationDTO markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Уведомление не найдено"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new RuntimeException("Нет доступа к этому уведомлению");
        }

        notification.setRead(true);
        notification = notificationRepository.save(notification);
        return convertToDTO(notification);
    }

    /**
     * Отметить все уведомления пользователя как прочитанные
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
        log.info("Marked all notifications as read for user {}", userId);
    }

    /**
     * Удалить уведомление
     */
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Уведомление не найдено"));

        if (!notification.getUser().getId().equals(userId)) {
            throw new RuntimeException("Нет доступа к этому уведомлению");
        }

        notificationRepository.delete(notification);
        log.info("Deleted notification {} for user {}", notificationId, userId);
    }

    // ─── DTO conversion ──────────────────────────────────────────────────────────

    public NotificationDTO convertToDTO(Notification notification) {
        return new NotificationDTO(
                notification.getId(),
                notification.getUser().getId(),
                notification.getType(),
                notification.getContent(),
                notification.getLink(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
