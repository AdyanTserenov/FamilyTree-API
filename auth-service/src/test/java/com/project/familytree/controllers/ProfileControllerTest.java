package com.project.familytree.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.familytree.configurators.SecurityConfig;
import com.project.familytree.dto.ChangePasswordRequest;
import com.project.familytree.dto.UpdateProfileRequest;
import com.project.familytree.exceptions.InvalidRequestException;
import com.project.familytree.models.User;
import com.project.familytree.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProfileController.class)
@Import(SecurityConfig.class)
class ProfileControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setFirstName("Иван");
        user.setLastName("Иванов");
        user.setMiddleName("Иванович");
        user.setEmail("user@test.com");
        user.setEnabled(true);
        // createdAt is set by @PrePersist in real usage; set manually for tests
        try {
            var field = User.class.getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(user, LocalDateTime.of(2024, 1, 1, 0, 0));
        } catch (Exception ignored) {}
    }

    // ─── GET /profile ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /profile → 200 с данными профиля")
    @WithMockUser(username = "user@test.com")
    void getProfile_returns200WithProfileData() throws Exception {
        when(userService.findByEmail("user@test.com")).thenReturn(user);

        mockMvc.perform(get("/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Иван"))
                .andExpect(jsonPath("$.data.lastName").value("Иванов"))
                .andExpect(jsonPath("$.data.email").value("user@test.com"))
                .andExpect(jsonPath("$.data.emailVerified").value(true));
    }

    @Test
    @DisplayName("GET /profile → 401 без аутентификации")
    void getProfile_returns401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().isUnauthorized());
    }

    // ─── PATCH /profile ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("PATCH /profile → 200 с обновлёнными данными")
    @WithMockUser(username = "user@test.com")
    void updateProfile_returns200WithUpdatedData() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Пётр");
        request.setLastName("Петров");
        request.setMiddleName("Петрович");

        User updated = new User();
        updated.setId(1L);
        updated.setFirstName("Пётр");
        updated.setLastName("Петров");
        updated.setMiddleName("Петрович");
        updated.setEmail("user@test.com");
        updated.setEnabled(true);

        when(userService.updateProfile(eq("user@test.com"), any(UpdateProfileRequest.class)))
                .thenReturn(updated);

        mockMvc.perform(patch("/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("Пётр"))
                .andExpect(jsonPath("$.data.lastName").value("Петров"))
                .andExpect(jsonPath("$.data.middleName").value("Петрович"));
    }

    @Test
    @DisplayName("PATCH /profile → 401 без аутентификации")
    void updateProfile_returns401WhenUnauthenticated() throws Exception {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Пётр");
        request.setLastName("Петров");

        mockMvc.perform(patch("/profile")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ─── POST /profile/change-password ────────────────────────────────────────────

    @Test
    @DisplayName("POST /profile/change-password → 200 при успешной смене пароля")
    @WithMockUser(username = "user@test.com")
    void changePassword_returns200OnSuccess() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("newPassword123");

        doNothing().when(userService).changePassword("user@test.com", "oldPassword", "newPassword123");

        mockMvc.perform(post("/profile/change-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value("Пароль успешно изменён"));
    }

    @Test
    @DisplayName("POST /profile/change-password → 400 при неверном текущем пароле")
    @WithMockUser(username = "user@test.com")
    void changePassword_returns400OnWrongCurrentPassword() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrongPassword");
        request.setNewPassword("newPassword123");

        doThrow(new InvalidRequestException("Текущий пароль неверен"))
                .when(userService).changePassword("user@test.com", "wrongPassword", "newPassword123");

        mockMvc.perform(post("/profile/change-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Текущий пароль неверен"));
    }

    @Test
    @DisplayName("POST /profile/change-password → 401 без аутентификации")
    void changePassword_returns401WhenUnauthenticated() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("newPassword123");

        mockMvc.perform(post("/profile/change-password")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
