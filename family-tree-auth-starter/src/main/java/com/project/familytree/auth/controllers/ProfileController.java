package com.project.familytree.auth.controllers;

import com.project.familytree.auth.dto.CustomApiResponse;
import com.project.familytree.auth.dto.ProfileResponse;
import com.project.familytree.auth.models.User;
import com.project.familytree.auth.services.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/profile")
@Tag(name = "Profile Controller", description = "API для управления профилем пользователя")
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    private final UserService userService;

    @Autowired
    public ProfileController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<CustomApiResponse<ProfileResponse>> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        ProfileResponse response = new ProfileResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getMiddleName(),
                user.getEmail(),
                user.getEnabled()
        );
        return ResponseEntity.ok(CustomApiResponse.successData(response));
    }

    @PutMapping
    public ResponseEntity<CustomApiResponse<String>> updateProfile(@AuthenticationPrincipal UserDetails userDetails,
                                                                   @RequestBody ProfileResponse profileRequest) {
        // Logic to update profile
        return ResponseEntity.ok(CustomApiResponse.successMessage("Профиль обновлён"));
    }
}