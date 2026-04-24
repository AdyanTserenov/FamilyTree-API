package com.project.familytree.tree.dto;

import java.util.List;

/**
 * Ответ с пагинацией для комментариев
 */
public class PagedCommentsResponse {

    private List<CommentDTO> data;
    private long totalCount;
    private boolean hasMore;

    public PagedCommentsResponse() {}

    public PagedCommentsResponse(List<CommentDTO> data, long totalCount, boolean hasMore) {
        this.data = data;
        this.totalCount = totalCount;
        this.hasMore = hasMore;
    }

    public List<CommentDTO> getData() {
        return data;
    }

    public void setData(List<CommentDTO> data) {
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
