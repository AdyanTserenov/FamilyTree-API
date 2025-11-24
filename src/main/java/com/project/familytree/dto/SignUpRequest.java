package com.project.familytree.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "DTO для запроса на регистрацию")
public class SignUpRequest {
    @Schema(description = "Имя пользователя", example = "Адьян")
    @NotBlank(message = "Имя обязательно")
    private String firstName;

    @Schema(description = "Фамилия пользователя", example = "Церенов")
    @NotBlank(message = "Фамилия обязательна")
    private String lastName;

    @Schema(description = "Отчество пользователя", example = "Баатрович")
    private String middleName;

    @Schema(description = "Почта пользователя", example = "a-tserenov@mail.ru")
    @NotBlank(message = "Email обязателен")
    @Email(message = "Некорректный формат email")
    private String email;

    @Schema(description = "Пароль для входа", example = "test123")
    @NotBlank(message = "Пароль обязателен")
    @Size(min = 6, message = "Пароль должен быть больше 6 символов")
    private String password;
}
