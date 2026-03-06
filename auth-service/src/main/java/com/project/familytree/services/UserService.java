package com.project.familytree.services;

import com.project.familytree.dto.SignUpRequest;
import com.project.familytree.dto.UpdateProfileRequest;
import com.project.familytree.exceptions.*;
import com.project.familytree.impls.TokenType;
import com.project.familytree.impls.UserDetailsImpl;
import com.project.familytree.models.User;
import com.project.familytree.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailSenderService mailSenderService;
    private final TokenService tokenService;

    /**
     * When false, users are auto-enabled on registration (no email verification required).
     * Set APP_EMAIL_VERIFICATION_REQUIRED=false in .env for demo/dev environments
     * where SMTP is not configured.
     * Default: true (email verification required).
     */
    @Value("${app.email-verification-required:true}")
    private boolean emailVerificationRequired;

    @Override
    public UserDetails loadUserByUsername(String email) throws EmailNotFoundException {
        User user = findByEmail(email);
        return UserDetailsImpl.build(user);
    }

    public User findByEmail(String email) throws EmailNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EmailNotFoundException("Почта не найдена"));
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с таким ID не существует"));
    }

    public Long findIdByDetails(UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("UserDetails cannot be null");
        }
        return findByEmail(userDetails.getUsername()).getId();
    }

    public void signUpUser(SignUpRequest signUpRequest) throws InvalidRequestException {
        if (userRepository.existsByEmail(signUpRequest.getEmail())) {
            throw new InvalidRequestException("Пользователь с таким email уже существует");
        }
        String hashed = passwordEncoder.encode(signUpRequest.getPassword());

        User user = new User();
        user.setFirstName(signUpRequest.getFirstName());
        user.setLastName(signUpRequest.getLastName());
        user.setMiddleName(signUpRequest.getMiddleName());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(hashed);

        if (!emailVerificationRequired) {
            // Demo/dev mode: skip email verification, auto-enable the user
            user.setEnabled(true);
            userRepository.save(user);
            log.info("Email verification disabled — user {} auto-enabled on registration", signUpRequest.getEmail());
        } else {
            user.setEnabled(false);
            userRepository.save(user);
            sendVerifyToken(user.getEmail());
        }
    }

    public void sendVerifyToken(String email) {
        User user = findByEmail(email);
        String token = tokenService.createVerifyToken(user.getId());

        try {
            mailSenderService.sendCreationEmail(email, token, user.getFirstName());
        } catch (EmailSenderException ex) {
            // Do NOT delete the user — they can use "Resend verification" once SMTP is fixed.
            // Deleting the user on SMTP failure causes data loss and confuses users.
            log.error("Failed to send verification email to {}: {}", email, ex.getMessage());
            throw new UserRegistrationFailedException("Ошибка регистрации: не удалось отправить письмо с подтверждением", ex);
        }
    }

    public void sendResetToken(String email) {
        User user = findByEmail(email);
        String token = tokenService.createResetToken(user.getId());

        mailSenderService.sendPasswordResetEmail(email, token, user.getFirstName());
    }

    public void confirmUser(String token) {
        Long userId = tokenService.validateToken(token, TokenType.VERIFY);

        User user = findById(userId);
        user.setEnabled(true);

        userRepository.save(user);
        tokenService.consumeToken(token);
    }

    public void resetPassword(String token, String newPassword) {
        Long userId = tokenService.validateToken(token, TokenType.RESET);

        User user = findById(userId);
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        tokenService.consumeToken(token);
    }

    public User updateProfile(String email, UpdateProfileRequest request) {
        User user = findByEmail(email);
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setMiddleName(request.getMiddleName());
        return userRepository.save(user);
    }

    public void changePassword(String email, String currentPassword, String newPassword) {
        User user = findByEmail(email);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new InvalidRequestException("Текущий пароль неверен");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public void resendVerification(String email) {
        User user = findByEmail(email);
        if (user.isEnabled()) {
            throw new InvalidRequestException("Email уже подтверждён");
        }
        sendVerifyToken(email);
    }

    public void deleteAccount(String email) {
        User user = findByEmail(email);
        tokenService.deleteAllTokensForUser(user.getId());
        userRepository.deleteById(user.getId());
        log.info("Account deleted for user: {}", email);
    }
}
