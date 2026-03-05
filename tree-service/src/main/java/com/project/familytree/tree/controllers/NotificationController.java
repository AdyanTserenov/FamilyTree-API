package com.project.familytree.tree.controllers;

import com.project.familytree.auth.dto.CustomApiResponse;
import com.project.familytree.auth.services.UserService;
import com.project.familytree.tree.dto.NotificationDTO;
import com.project.familytree.tree.services.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notification Controller", description = "API для управления уведомлениями пользователя")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationService notificationService;
    private final UserService userService;

    public NotificationController(NotificationService notificationService, UserService userService) {
        this.notificationService = notificationService;
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "Получить уведомления",
               description = "Возвращает все уведомления текущего пользователя. Непрочитанные идут первыми.")
    public ResponseEntity<CustomApiResponse<List<NotificationDTO>>> getNotifications() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        log.info("Getting notifications for user {}", userId);

        List<NotificationDTO> notifications = notificationService.getUserNotifications(userId);
        return ResponseEntity.ok(CustomApiResponse.successData(notifications));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Количество непрочитанных уведомлений",
               description = "Возвращает количество непрочитанных уведомлений текущего пользователя.")
    public ResponseEntity<CustomApiResponse<Map<String, Long>>> getUnreadCount() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);

        long count = notificationService.countUnread(userId);
        return ResponseEntity.ok(CustomApiResponse.successData(Map.of("unreadCount", count)));
    }

    @PutMapping("/{notificationId}/read")
    @Operation(summary = "Отметить уведомление как прочитанное",
               description = "Помечает одно уведомление как прочитанное.")
    public ResponseEntity<CustomApiResponse<NotificationDTO>> markAsRead(
            @PathVariable Long notificationId) throws AccessDeniedException {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        log.info("Marking notification {} as read for user {}", notificationId, userId);

        NotificationDTO dto = notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.ok(CustomApiResponse.successData(dto));
    }

    @PutMapping("/read-all")
    @Operation(summary = "Отметить все уведомления как прочитанные",
               description = "Помечает все уведомления текущего пользователя как прочитанные.")
    public ResponseEntity<CustomApiResponse<String>> markAllAsRead() {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        log.info("Marking all notifications as read for user {}", userId);

        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(CustomApiResponse.successMessage("Все уведомления отмечены как прочитанные"));
    }

    @DeleteMapping("/{notificationId}")
    @Operation(summary = "Удалить уведомление",
               description = "Удаляет уведомление текущего пользователя.")
    public ResponseEntity<CustomApiResponse<String>> deleteNotification(
            @PathVariable Long notificationId) throws AccessDeniedException {
        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        log.info("Deleting notification {} for user {}", notificationId, userId);

        notificationService.deleteNotification(notificationId, userId);
        return ResponseEntity.ok(CustomApiResponse.successMessage("Уведомление удалено"));
    }
}
