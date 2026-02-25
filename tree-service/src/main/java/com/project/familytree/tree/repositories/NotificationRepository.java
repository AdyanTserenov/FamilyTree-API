package com.project.familytree.tree.repositories;

import com.project.familytree.tree.models.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Все уведомления пользователя: сначала непрочитанные, затем по дате (новые первые)
     */
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId ORDER BY n.read ASC, n.createdAt DESC")
    List<Notification> findByUserIdOrderByReadAscCreatedAtDesc(@Param("userId") Long userId);

    /**
     * Количество непрочитанных уведомлений пользователя
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.read = false")
    long countUnreadByUserId(@Param("userId") Long userId);

    /**
     * Отметить все уведомления пользователя как прочитанные
     */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.user.id = :userId AND n.read = false")
    void markAllAsReadByUserId(@Param("userId") Long userId);
}
