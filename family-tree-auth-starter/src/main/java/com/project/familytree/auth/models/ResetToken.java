package com.project.familytree.auth.models;

public class ResetToken {
    private Long userId;

    public ResetToken() {
    }

    public ResetToken(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}