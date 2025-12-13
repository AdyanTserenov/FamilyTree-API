package com.project.familytree.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "DTO для установления нового пароля пользователя")
public class PasswordResetDTO {
    @Schema(description = "Токен сброса пароля")
    @NotBlank(message = "Токен обязателен")
    private String token;

    @Schema(description = "Новый пароль", example = "new_test123")
    @NotBlank(message = "Новый пароль обязателен")
    @Size(min = 6, message = "Пароль должен быть больше 6 символов")
    private String newPassword;
}
