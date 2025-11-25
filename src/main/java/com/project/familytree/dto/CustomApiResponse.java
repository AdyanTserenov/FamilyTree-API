package com.project.familytree.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Стандартизированный ответ API")
public class CustomApiResponse<T> {
    @Schema(description = "Статус ответа", example = "success")
    private String status;

    @Schema(description = "Данные ответа")
    private T data;

    @Schema(description = "Описание ошибки (если есть)", example = "User not found")
    private String error;

    @Schema(description = "Дополнительные детали (например для ошибок валидации)")
    private Map<String, Object> details;

    public static <T> CustomApiResponse<T> success(T data) {
        return new CustomApiResponse<>("success", data, null, null);
    }

    public static <T> CustomApiResponse<T> error(String error, Map<String, Object> details) {
        return new CustomApiResponse<>("error", null, error, details);
    }
}