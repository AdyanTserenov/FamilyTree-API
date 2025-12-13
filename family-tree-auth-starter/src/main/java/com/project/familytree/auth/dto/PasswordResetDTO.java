package com.project.familytree.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordResetDTO {

    @Schema(description = "Email пользователя")
    @NotBlank(message = "Email обязателен")
    @Email(message = "Неверный формат email")
    private String email;
}