package com.project.familytree.exceptions;

import com.project.familytree.dto.CustomApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.nio.file.AccessDeniedException;
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
        return ResponseEntity.badRequest().body(CustomApiResponse.error("Ошибка валидации", errors));
    }

    @ExceptionHandler(
                {
                    UserNotFoundException.class,
                    EmailNotFoundException.class,
                    TokenNotFoundException.class,
                    TreeNotFoundException.class
                }
            )
    public ResponseEntity<CustomApiResponse<?>> handleNotFoundException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(CustomApiResponse.error(ex.getMessage(), null));
    }

    @ExceptionHandler({InvalidRequestException.class, InvalidTokenException.class})
    public ResponseEntity<CustomApiResponse<?>> handleInvalidException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(CustomApiResponse.error(ex.getMessage(), null));
    }

    @ExceptionHandler(UserRegistrationFailedException.class)
    public ResponseEntity<CustomApiResponse<?>> handleRegistrationFailed(UserRegistrationFailedException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CustomApiResponse.error("Не удалось завершить регистрацию", null));
    }

    @ExceptionHandler(EmailSenderException.class)
    public ResponseEntity<CustomApiResponse<?>> handleEmailSenderException(EmailSenderException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CustomApiResponse.error("Не удалось отправить письмо для сброса пароля", null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<CustomApiResponse<?>> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(CustomApiResponse.error("Недостаточно прав для выполнения операции", null));
    }

    /** Handles Spring Security authentication failures from AuthenticationManager.authenticate() */
    @ExceptionHandler(InternalAuthenticationServiceException.class)
    public ResponseEntity<CustomApiResponse<Void>> handleInternalAuthException(InternalAuthenticationServiceException ex) {
        Throwable cause = ex.getCause();
        if (cause instanceof DisabledException) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(CustomApiResponse.error("Email не подтверждён. Проверьте почту и перейдите по ссылке для активации.", null));
        }
        // EmailNotFoundException or any other cause → treat as bad credentials
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(CustomApiResponse.error("Неверный email или пароль", null));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<CustomApiResponse<Void>> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(CustomApiResponse.error("Неверный email или пароль", null));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<CustomApiResponse<Void>> handleDisabled(DisabledException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(CustomApiResponse.error("Email не подтверждён. Проверьте почту и перейдите по ссылке для активации.", null));
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<CustomApiResponse<Void>> handleLocked(LockedException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(CustomApiResponse.error("Аккаунт заблокирован", null));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<CustomApiResponse<Void>> handleGeneric(RuntimeException ex) {
        log.error("Непредвиденная ошибка", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(CustomApiResponse.error("Internal server error", null));
    }
}
