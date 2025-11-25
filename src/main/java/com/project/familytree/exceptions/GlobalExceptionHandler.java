package com.project.familytree.exceptions;

import com.project.familytree.dto.CustomApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.HandlerMethod;

import java.util.HashMap;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CustomApiResponse<?>> handleValidationException(MethodArgumentNotValidException ex) {
        var errors = new HashMap<String, Object>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.badRequest().body(CustomApiResponse.error("Validation failed", errors));
    }

    @ExceptionHandler({UserNotFoundException.class, EmailNotFound.class})
    public ResponseEntity<CustomApiResponse<?>> handleNotFoundException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(CustomApiResponse.error(ex.getMessage(), null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CustomApiResponse<Void>> handleGeneric(Exception ex, HandlerMethod handlerMethod) {
        // Пропускаем ошибки, связанные с документацией
        if (handlerMethod != null) {
            String className = handlerMethod.getBeanType().getName();
            if (className.contains("OpenApiResource") || className.contains("Swagger")) {
                throw new RuntimeException(ex); // не перехватываем — пусть упадёт как есть
            }
        }
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CustomApiResponse.error("Internal server error", null));
    }
}
