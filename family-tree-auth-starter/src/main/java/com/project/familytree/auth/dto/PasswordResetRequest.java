package com.project.familytree.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordResetRequest {

    @Schema(description = "Новый пароль")
    @NotBlank(message = "Пароль обязателен")
    @Size(min = 6, max = 100, message = "Пароль должен быть от 6 до 100 символов")
    private String password;

    @Schema(description = "Токен сброса пароля")
    @NotBlank(message = "Токен обязателен")
    private String token;

    public String getPassword() {
        return password;
    }

    public String getToken() {
        return token;
    }
}