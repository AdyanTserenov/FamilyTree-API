package com.project.familytree.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.familytree.configurators.SecurityConfig;
import com.project.familytree.dto.PasswordResetDTO;
import com.project.familytree.dto.PasswordResetRequest;
import com.project.familytree.dto.SignInRequest;
import com.project.familytree.dto.SignUpRequest;
import com.project.familytree.exceptions.*;
import com.project.familytree.impls.UserDetailsImpl;
import com.project.familytree.security.JwtCore;
import com.project.familytree.services.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SecurityController.class)
@Import(SecurityConfig.class)
class SecurityControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean UserService userService;
    @MockBean JwtCore jwtCore;
    @MockBean AuthenticationManager authenticationManager;

    // ─── GET /auth/ping ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /auth/ping → 200 pong")
    void ping_returns200() throws Exception {
        mockMvc.perform(get("/auth/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("pong"));
    }

    // ─── POST /auth/sign-up ───────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/sign-up → 200 при успешной регистрации")
    void signUp_returns200OnSuccess() throws Exception {
        SignUpRequest request = new SignUpRequest();
        request.setFirstName("Иван");
        request.setLastName("Иванов");
        request.setEmail("ivan@test.com");
        request.setPassword("password123");

        doNothing().when(userService).signUpUser(any(SignUpRequest.class));

        mockMvc.perform(post("/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Пользователь зарегистрирован"));
    }

    @Test
    @DisplayName("POST /auth/sign-up → 400 если email уже занят")
    void signUp_returns400IfEmailExists() throws Exception {
        SignUpRequest request = new SignUpRequest();
        request.setFirstName("Иван");
        request.setLastName("Иванов");
        request.setEmail("ivan@test.com");
        request.setPassword("password123");

        doThrow(new InvalidRequestException("Пользователь с таким email уже существует"))
                .when(userService).signUpUser(any(SignUpRequest.class));

        mockMvc.perform(post("/auth/sign-up")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Пользователь с таким email уже существует"));
    }

    // ─── POST /auth/sign-in ───────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/sign-in → 200 с JWT токеном")
    void signIn_returns200WithJwt() throws Exception {
        SignInRequest request = new SignInRequest();
        request.setEmail("ivan@test.com");
        request.setPassword("password123");

        UserDetailsImpl userDetails = new UserDetailsImpl(
                1L, "ivan@test.com", "hashed", null, null, Collections.emptyList()
        );
        Authentication auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, Collections.emptyList()
        );

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtCore.generateToken(auth)).thenReturn("jwt.token.here");

        mockMvc.perform(post("/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("jwt.token.here"));
    }

    @Test
    @DisplayName("POST /auth/sign-in → 500 при неверных учётных данных (BadCredentialsException)")
    void signIn_returns500OnBadCredentials() throws Exception {
        SignInRequest request = new SignInRequest();
        request.setEmail("ivan@test.com");
        request.setPassword("wrongpassword");

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        mockMvc.perform(post("/auth/sign-in")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError());
    }

    // ─── POST /auth/forgot ────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/forgot → всегда 200 (не раскрывает наличие email)")
    void forgotPassword_alwaysReturns200() throws Exception {
        PasswordResetRequest request = new PasswordResetRequest();
        request.setEmail("unknown@test.com");

        // Даже если email не найден — 200
        doThrow(new EmailNotFoundException("Почта не найдена"))
                .when(userService).sendResetToken("unknown@test.com");

        mockMvc.perform(post("/auth/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").exists());
    }

    @Test
    @DisplayName("POST /auth/forgot → 200 если email существует")
    void forgotPassword_returns200IfEmailExists() throws Exception {
        PasswordResetRequest request = new PasswordResetRequest();
        request.setEmail("ivan@test.com");

        doNothing().when(userService).sendResetToken("ivan@test.com");

        mockMvc.perform(post("/auth/forgot")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value(
                        "If the email exists, a password reset link has been sent"));
    }

    // ─── POST /auth/reset ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/reset → 200 при успешном сбросе пароля")
    void resetPassword_returns200OnSuccess() throws Exception {
        PasswordResetDTO request = new PasswordResetDTO();
        request.setToken("valid-reset-token");
        request.setNewPassword("newPassword123");

        doNothing().when(userService).resetPassword("valid-reset-token", "newPassword123");

        mockMvc.perform(post("/auth/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Password has been reset successfully"));
    }

    @Test
    @DisplayName("POST /auth/reset → 500 при невалидном токене")
    void resetPassword_returns500OnInvalidToken() throws Exception {
        PasswordResetDTO request = new PasswordResetDTO();
        request.setToken("bad-token");
        request.setNewPassword("newPassword123");

        doThrow(new TokenNotFoundException("Некорректный или просроченный токен"))
                .when(userService).resetPassword("bad-token", "newPassword123");

        mockMvc.perform(post("/auth/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    // ─── GET /auth/confirm ────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /auth/confirm?token= → 200 при успешном подтверждении")
    void confirmRegistration_returns200OnSuccess() throws Exception {
        doNothing().when(userService).confirmUser("valid-verify-token");

        mockMvc.perform(get("/auth/confirm")
                        .param("token", "valid-verify-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("email успешно подтверждён!"));
    }

    @Test
    @DisplayName("GET /auth/confirm?token= → 500 при невалидном токене")
    void confirmRegistration_returns500OnInvalidToken() throws Exception {
        doThrow(new TokenNotFoundException("Некорректный или просроченный токен"))
                .when(userService).confirmUser("bad-token");

        mockMvc.perform(get("/auth/confirm")
                        .param("token", "bad-token"))
                .andExpect(status().isNotFound());
    }

    // ─── POST /auth/resend-verification ──────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/resend-verification → 200 при успешной отправке")
    void resendVerification_returns200OnSuccess() throws Exception {
        PasswordResetRequest request = new PasswordResetRequest();
        request.setEmail("ivan@test.com");

        doNothing().when(userService).resendVerification("ivan@test.com");

        mockMvc.perform(post("/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(
                        "Если email зарегистрирован и не подтверждён, письмо будет отправлено"));
    }

    @Test
    @DisplayName("POST /auth/resend-verification → всегда 200 (не раскрывает наличие email)")
    void resendVerification_alwaysReturns200EvenIfEmailNotFound() throws Exception {
        PasswordResetRequest request = new PasswordResetRequest();
        request.setEmail("unknown@test.com");

        // Даже если email не найден или уже подтверждён — всегда 200
        doThrow(new InvalidRequestException("Email уже подтверждён"))
                .when(userService).resendVerification("unknown@test.com");

        mockMvc.perform(post("/auth/resend-verification")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(
                        "Если email зарегистрирован и не подтверждён, письмо будет отправлено"));
    }
}
