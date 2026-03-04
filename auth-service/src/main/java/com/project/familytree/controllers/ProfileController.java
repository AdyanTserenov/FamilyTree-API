package com.project.familytree.controllers;

import com.project.familytree.dto.ChangePasswordRequest;
import com.project.familytree.dto.CustomApiResponse;
import com.project.familytree.dto.ProfileResponse;
import com.project.familytree.dto.UpdateProfileRequest;
import com.project.familytree.models.User;
import com.project.familytree.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/profile")
@Tag(name = "Profile controller", description = "API для личного кабинета")
public class ProfileController {
    private final UserService userService;

    @GetMapping
    @Operation(summary = "Получить информацию о себе по JWT")
    public ResponseEntity<CustomApiResponse<ProfileResponse>> getInfo(@AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        User user = userService.findByEmail(email);

        ProfileResponse profileResponse = new ProfileResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getMiddleName(),
                user.getEmail(),
                user.isEnabled(),
                user.getCreatedAt()
        );
        return ResponseEntity.ok(CustomApiResponse.success(profileResponse));
    }

    @PatchMapping
    @Operation(summary = "Обновить имя/фамилию/отчество профиля")
    public ResponseEntity<CustomApiResponse<ProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        String email = userDetails.getUsername();
        User updated = userService.updateProfile(email, request);

        ProfileResponse profileResponse = new ProfileResponse(
                updated.getId(),
                updated.getFirstName(),
                updated.getLastName(),
                updated.getMiddleName(),
                updated.getEmail(),
                updated.isEnabled(),
                updated.getCreatedAt()
        );
        return ResponseEntity.ok(CustomApiResponse.success(profileResponse));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Изменить пароль (требует текущий пароль)")
    public ResponseEntity<CustomApiResponse<String>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        String email = userDetails.getUsername();
        userService.changePassword(email, request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(CustomApiResponse.success("Пароль успешно изменён"));
    }

    @DeleteMapping
    @Operation(summary = "Удалить аккаунт текущего пользователя")
    public ResponseEntity<CustomApiResponse<String>> deleteAccount(
            @AuthenticationPrincipal UserDetails userDetails) {
        String email = userDetails.getUsername();
        userService.deleteAccount(email);
        return ResponseEntity.ok(CustomApiResponse.success("Аккаунт успешно удалён"));
    }
}
