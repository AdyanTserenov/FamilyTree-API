package com.project.familytree.tree.impls;

/**
 * Типы уведомлений в системе семейного дерева
 */
public enum NotificationType {
    /** Добавлен новый комментарий к персоне */
    COMMENT_ADDED,
    /** Добавлена новая персона в дерево */
    PERSON_ADDED,
    /** Данные персоны обновлены */
    PERSON_UPDATED,
    /** Новый участник принял приглашение */
    MEMBER_JOINED,
    /** Отправлено приглашение */
    INVITATION_SENT
}
