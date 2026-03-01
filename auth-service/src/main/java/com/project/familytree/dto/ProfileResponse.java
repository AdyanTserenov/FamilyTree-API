package com.project.familytree.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "DTO для запроса-ответа для личного кабинета")
@AllArgsConstructor
public class ProfileResponse {
    @Schema(description = "ID пользователя")
    private Long id;

    @Schema(description = "Имя пользователя", example = "Иван")
    private String firstName;

    @Schema(description = "Фамилия пользователя", example = "Иванов")
    private String lastName;

    @Schema(description = "Отчество пользователя", example = "Иванович")
    private String middleName;

    @Schema(description = "Почта пользователя", example = "ivanov@mail.ru")
    private String email;

    @Schema(description = "Подтверждён ли email")
    private boolean emailVerified;

    @Schema(description = "Дата регистрации")
    private LocalDateTime createdAt;
}
