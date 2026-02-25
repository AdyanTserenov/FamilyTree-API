package com.project.familytree.tree.exceptions;

import com.project.familytree.auth.dto.CustomApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.nio.file.AccessDeniedException;
import java.util.HashMap;
import java.util.Map;

/**
 * Глобальный обработчик исключений для tree-service.
 * Преобразует исключения в стандартный формат CustomApiResponse.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 403 — Нет прав доступа
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<CustomApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(CustomApiResponse.error(ex.getMessage()));
    }

    /**
     * 400 — Ошибки валидации полей (@Valid)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CustomApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("Validation failed: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CustomApiResponse.success("Ошибка валидации", errors));
    }

    /**
     * 413 — Превышен размер загружаемого файла
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<CustomApiResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException ex) {
        log.warn("File too large: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(CustomApiResponse.error("Файл слишком большой. Максимальный размер: 50 МБ"));
    }

    /**
     * 404 — Ресурс не найден (RuntimeException с "не найден" в сообщении)
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<CustomApiResponse<Void>> handleRuntime(RuntimeException ex) {
        String message = ex.getMessage();
        if (message != null && (message.contains("не найден") || message.contains("не найдена"))) {
            log.warn("Resource not found: {}", message);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(CustomApiResponse.error(message));
        }
        log.error("Runtime error: {}", message, ex);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CustomApiResponse.error(message != null ? message : "Произошла ошибка"));
    }

    /**
     * 500 — Непредвиденная ошибка
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<CustomApiResponse<Void>> handleGeneral(Exception ex) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CustomApiResponse.error("Внутренняя ошибка сервера"));
    }
}
