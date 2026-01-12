package com.project.familytree.auth.controllers;

import com.project.familytree.auth.dto.*;
import com.project.familytree.auth.models.User;
import com.project.familytree.auth.security.JwtUtils;
import com.project.familytree.auth.services.TokenService;
import com.project.familytree.auth.services.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Security Controller", description = "API для аутентификации и авторизации")
public class SecurityController {

    private static final Logger log = LoggerFactory.getLogger(SecurityController.class);

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    public SecurityController(AuthenticationManager authenticationManager, UserService userService, TokenService tokenService, PasswordEncoder passwordEncoder, JwtUtils jwtUtils) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
    }

    @PostMapping("/signin")
    public ResponseEntity<CustomApiResponse<String>> authenticateUser(@Valid @RequestBody SignInRequest signInRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(signInRequest.getEmail(), signInRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = jwtUtils.generateJwtToken(authentication);

        return ResponseEntity.ok(CustomApiResponse.success("Вход выполнен успешно", jwt));
    }

    @PostMapping("/signup")
    public ResponseEntity<CustomApiResponse<String>> registerUser(@Valid @RequestBody SignUpRequest signUpRequest) {
        if (userService.existsByEmail(signUpRequest.getEmail())) {
            return ResponseEntity.badRequest().body(CustomApiResponse.error("Email уже используется"));
        }

        // Create user logic here
        com.project.familytree.auth.models.User user = new com.project.familytree.auth.models.User();
        user.setFirstName(signUpRequest.getFirstName());
        user.setLastName(signUpRequest.getLastName());
        user.setMiddleName(signUpRequest.getMiddleName());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        user.setEnabled(true); // For demo, enable immediately
        userService.save(user);
        var tokenDetails = tokenService.createVerifyToken(user);

        // Send verification email
        String verificationLink = "http://localhost:8080/auth/confirm?token=" + tokenDetails.getToken();
        String subject = "Подтверждение email";
        String text = "Пожалуйста, подтвердите ваш email, перейдя по ссылке: " + verificationLink;
        // Assuming MailSenderService is available; for now, log it
        log.info("Verification email to {}: {}", user.getEmail(), text);

        return ResponseEntity.ok(CustomApiResponse.successMessage("Регистрация успешна"));
    }

    @GetMapping("/confirm")
    public ResponseEntity<CustomApiResponse<String>> confirmRegistration(@RequestParam("token") String token) {
        userService.confirmUser(token);
        return ResponseEntity.ok(CustomApiResponse.successMessage("Email подтверждён"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<CustomApiResponse<String>> resetPassword(@Valid @RequestBody PasswordResetDTO passwordResetDTO) {
        if (!userService.existsByEmail(passwordResetDTO.getEmail())) {
            return ResponseEntity.badRequest().body(CustomApiResponse.error("Email не найден"));
        }

        User user = userService.findByEmail(passwordResetDTO.getEmail());
        var tokenDetails = tokenService.createResetToken(user);

        // Send reset email
        String resetLink = "http://localhost:8080/auth/confirm-reset?token=" + tokenDetails.getToken();
        String subject = "Сброс пароля";
        String text = "Для сброса пароля перейдите по ссылке: " + resetLink;
        log.info("Reset email to {}: {}", user.getEmail(), text);

        return ResponseEntity.ok(CustomApiResponse.successMessage("Инструкции отправлены на email"));
    }

    @PostMapping("/confirm-reset")
    public ResponseEntity<CustomApiResponse<String>> confirmReset(@Valid @RequestBody PasswordResetRequest passwordResetRequest) {
        userService.resetPassword(passwordResetRequest.getToken(), passwordResetRequest.getPassword());
        return ResponseEntity.ok(CustomApiResponse.successMessage("Пароль изменён"));
    }
}