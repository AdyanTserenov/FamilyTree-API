package com.project.familytree.auth.models;

public class VerifyToken {
    private Long userId;

    public VerifyToken() {
    }

    public VerifyToken(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}