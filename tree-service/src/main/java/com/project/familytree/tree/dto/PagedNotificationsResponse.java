package com.project.familytree.tree.dto;

import java.util.List;

/**
 * Ответ с пагинацией для уведомлений
 */
public class PagedNotificationsResponse {

    private List<NotificationDTO> data;
    private long totalCount;
    private boolean hasMore;

    public PagedNotificationsResponse() {}

    public PagedNotificationsResponse(List<NotificationDTO> data, long totalCount, boolean hasMore) {
        this.data = data;
        this.totalCount = totalCount;
        this.hasMore = hasMore;
    }

    public List<NotificationDTO> getData() {
        return data;
    }

    public void setData(List<NotificationDTO> data) {
        this.data = data;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = totalCount;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }
}
