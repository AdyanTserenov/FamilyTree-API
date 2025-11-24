package com.project.familytree.controllers;

import com.project.familytree.dto.*;
import com.project.familytree.exceptions.InvalidRequestException;
import com.project.familytree.exceptions.InvalidTokenException;
import com.project.familytree.exceptions.UserNotFoundException;
import com.project.familytree.security.JwtCore;
import com.project.familytree.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;


@Slf4j
@Validated
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "API для регистрации и авторизации пользователей")
public class SecurityController {
    private final JwtCore jwtCore;
    private final UserService userService;
    private final AuthenticationManager authenticationManager;

    @GetMapping("/ping")
    public ResponseEntity<?> ping() {
        return ResponseEntity.ok("pong");
    }

    @PostMapping("/sign-up")
    @Operation(
            summary = "Регистрация пользователя",
            description = "Создает нового пользователя. Отдаёт всегда ApiResponse.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                {
                  "firstName": "Адьян",
                  "lastName": "Церенов",
                  "middleName": "Баатрович",
                  "email": "a-tserenov@mail.ru",
                  "password": "test123"
                }
                """
                            )
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Регистрация успешна",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                    {
                      "status": "success",
                      "data": "Пользователь зарегистрирован",
                      "error": null,
                      "details": null
                    }
                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Пользователь уже существует",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                    {
                      "status": "error",
                      "data": null,
                      "error": "Пользователь с таким email уже существует",
                      "details": null
                    }
                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<CustomApiResponse<String>> signup(@Valid @RequestBody SignUpRequest signupRequest) {
        userService.signUpUser(signupRequest);
        return ResponseEntity.ok(CustomApiResponse.success("Пользователь зарегистрирован"));
    }

    @PostMapping("/sign-in")
    @Operation(
            summary = "Логин пользователя",
            description = "Вход по email и паролю. Возвращает JWT в поле data ApiResponse.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = SignInRequest.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Успешный вход",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                    {
                      "status": "success",
                      "data": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                      "error": null,
                      "details": null
                    }
                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Ошибка аутентификации",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                    {
                      "status": "error",
                      "data": null,
                      "error": "Неверные учетные данные",
                      "details": null
                    }
                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<CustomApiResponse<String>> signin(@Valid @RequestBody SignInRequest signinRequest) {
    Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            signinRequest.getEmail(),
                            signinRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtCore.generateToken(authentication);
        return ResponseEntity.ok(CustomApiResponse.success(jwt));
    }

    @PostMapping("/forgot")
    @Operation(
            summary = "Запрос на сброс пароля",
            description = "Отправляет ссылку для сброса пароля, если email присутствует.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = PasswordResetRequest.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Ответ всегда один (не палит наличие email)",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                    {
                      "status": "success",
                      "data": {
                        "message": "If the email exists, a password reset link has been sent"
                      },
                      "error": null,
                      "details": null
                    }
                    """
                                    )
                            )
                    )
            }
    )
    public ResponseEntity<CustomApiResponse<Map<String, String>>> forgotPassword(@Valid @RequestBody PasswordResetRequest request) {
        userService.sendResetToken(request.getEmail());
        return ResponseEntity.ok(CustomApiResponse.success(Map.of("message", "If the email exists, a password reset link has been sent")));
    }

    @PostMapping("/reset")
    @Operation(
            summary = "Сброс пароля по токену",
            description = "Меняет пароль по валидному reset-токену.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = PasswordResetDTO.class)
                    )
            ),
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Пароль сброшен",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                    {
                      "status": "success",
                      "data": {
                        "message": "Password has been reset successfully"
                      },
                      "error": null,
                      "details": null
                    }
                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Некорректный токен или пользователь",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                    {
                      "status": "error",
                      "data": null,
                      "error": "Invalid or expired reset token",
                      "details": null
                    }
                    """
                                    )
                            )
                    )
            }
    )
    public ResponseEntity<CustomApiResponse<Map<String, String>>> resetPassword(@Valid @RequestBody PasswordResetDTO request) {
        userService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(CustomApiResponse.success(Map.of("message", "Password has been reset successfully")));
    }

    @GetMapping("/confirm")
    @Operation(
            summary = "Подтверждение почты по токену",
            description = "Верифицирует пользователя по токену из письма.",
            parameters = {
                    @Parameter(in = ParameterIn.QUERY, name = "token", required = true)
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Почта подтверждена",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                    {
                      "status": "success",
                      "data": "email успешно подтверждён!",
                      "error": null,
                      "details": null
                    }
                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Неверный или просроченный токен",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                    {
                      "status": "error",
                      "data": null,
                      "error": "Неверный или просроченный токен",
                      "details": null
                    }
                    """
                                    )
                            )
                    )
            }
    )
    public ResponseEntity<CustomApiResponse<String>> confirmRegistration(@RequestParam("token") String token) {
        userService.confirmUser(token);
        return ResponseEntity.ok(CustomApiResponse.success("email успешно подтверждён!"));
    }
}
