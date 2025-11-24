package com.project.familytree.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@Schema(description = "DTO для запроса-ответа для личного кабинета")
@AllArgsConstructor
public class ProfileResponse {
    @Schema(description = "Имя пользователя", example = "Иван")
    private String firstName;

    @Schema(description = "Фамилия пользователя", example = "Иванов")
    private String lastName;

    @Schema(description = "Отчество пользователя", example = "Иванович")
    private String middleName;

    @Schema(description = "Почта пользователя", example = "ivanov@mail.ru")
    private String email;
}
