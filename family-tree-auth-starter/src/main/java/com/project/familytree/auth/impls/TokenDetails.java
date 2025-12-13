package com.project.familytree.auth.impls;

import java.time.LocalDateTime;

public class TokenDetails {
    private String token;
    private LocalDateTime expiresAt;

    public TokenDetails() {
    }

    public TokenDetails(String token, LocalDateTime expiresAt) {
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}