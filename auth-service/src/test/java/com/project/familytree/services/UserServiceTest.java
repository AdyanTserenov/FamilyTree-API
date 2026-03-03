package com.project.familytree.services;

import com.project.familytree.dto.SignUpRequest;
import com.project.familytree.dto.UpdateProfileRequest;
import com.project.familytree.exceptions.*;
import com.project.familytree.impls.TokenType;
import com.project.familytree.models.User;
import com.project.familytree.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private MailSenderService mailSenderService;
    @Mock private TokenService tokenService;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setFirstName("Иван");
        user.setLastName("Иванов");
        user.setMiddleName("Иванович");
        user.setEmail("ivan@test.com");
        user.setPassword("hashed_password");
        user.setEnabled(false);
    }

    // ─── signUpUser ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("signUpUser: сохраняет пользователя и отправляет письмо")
    void signUpUser_savesUserAndSendsEmail() {
        SignUpRequest request = new SignUpRequest();
        request.setFirstName("Иван");
        request.setLastName("Иванов");
        request.setMiddleName("Иванович");
        request.setEmail("ivan@test.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("ivan@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(userRepository.findByEmail("ivan@test.com")).thenReturn(Optional.of(user));
        when(tokenService.createVerifyToken(anyLong())).thenReturn("verify-token-123");

        userService.signUpUser(request);

        verify(userRepository).save(any(User.class));
        verify(mailSenderService).sendCreationEmail(eq("ivan@test.com"), eq("verify-token-123"), eq("Иван"));
    }

    @Test
    @DisplayName("signUpUser: бросает InvalidRequestException если email уже занят")
    void signUpUser_throwsIfEmailExists() {
        SignUpRequest request = new SignUpRequest();
        request.setEmail("ivan@test.com");
        request.setPassword("password123");
        request.setFirstName("Иван");
        request.setLastName("Иванов");

        when(userRepository.existsByEmail("ivan@test.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.signUpUser(request))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("уже существует");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("signUpUser: удаляет пользователя и бросает исключение если письмо не отправлено")
    void signUpUser_deletesUserIfMailFails() {
        SignUpRequest request = new SignUpRequest();
        request.setFirstName("Иван");
        request.setLastName("Иванов");
        request.setEmail("ivan@test.com");
        request.setPassword("password123");

        when(userRepository.existsByEmail("ivan@test.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(userRepository.findByEmail("ivan@test.com")).thenReturn(Optional.of(user));
        when(tokenService.createVerifyToken(anyLong())).thenReturn("token");
        doThrow(new EmailSenderException("SMTP error"))
                .when(mailSenderService).sendCreationEmail(anyString(), anyString(), anyString());

        assertThatThrownBy(() -> userService.signUpUser(request))
                .isInstanceOf(UserRegistrationFailedException.class);

        verify(userRepository).delete(user);
    }

    // ─── confirmUser ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("confirmUser: активирует пользователя и потребляет токен")
    void confirmUser_enablesUserAndConsumesToken() {
        when(tokenService.validateToken("verify-token", TokenType.VERIFY)).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.confirmUser("verify-token");

        assertThat(user.isEnabled()).isTrue();
        verify(userRepository).save(user);
        verify(tokenService).consumeToken("verify-token");
    }

    // ─── resetPassword ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("resetPassword: кодирует новый пароль и потребляет токен")
    void resetPassword_encodesAndSaves() {
        when(tokenService.validateToken("reset-token", TokenType.RESET)).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("newPassword")).thenReturn("new_hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.resetPassword("reset-token", "newPassword");

        assertThat(user.getPassword()).isEqualTo("new_hashed");
        verify(tokenService).consumeToken("reset-token");
    }

    // ─── changePassword ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("changePassword: бросает InvalidRequestException если текущий пароль неверен")
    void changePassword_throwsIfWrongCurrentPassword() {
        when(userRepository.findByEmail("ivan@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "hashed_password")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword("ivan@test.com", "wrongPassword", "newPassword"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("неверен");
    }

    @Test
    @DisplayName("changePassword: кодирует и сохраняет новый пароль")
    void changePassword_encodesAndSavesNewPassword() {
        when(userRepository.findByEmail("ivan@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("currentPassword", "hashed_password")).thenReturn(true);
        when(passwordEncoder.encode("newPassword")).thenReturn("new_hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.changePassword("ivan@test.com", "currentPassword", "newPassword");

        assertThat(user.getPassword()).isEqualTo("new_hashed");
        verify(userRepository).save(user);
    }

    // ─── resendVerification ───────────────────────────────────────────────────────

    @Test
    @DisplayName("resendVerification: бросает InvalidRequestException если email уже подтверждён")
    void resendVerification_throwsIfAlreadyEnabled() {
        user.setEnabled(true);
        when(userRepository.findByEmail("ivan@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userService.resendVerification("ivan@test.com"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("уже подтверждён");
    }

    @Test
    @DisplayName("resendVerification: отправляет письмо если email не подтверждён")
    void resendVerification_sendsEmailIfNotEnabled() {
        user.setEnabled(false);
        when(userRepository.findByEmail("ivan@test.com")).thenReturn(Optional.of(user));
        when(tokenService.createVerifyToken(1L)).thenReturn("new-token");

        userService.resendVerification("ivan@test.com");

        verify(mailSenderService).sendCreationEmail("ivan@test.com", "new-token", "Иван");
    }

    // ─── updateProfile ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateProfile: обновляет имя, фамилию и отчество")
    void updateProfile_updatesNameFields() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Пётр");
        request.setLastName("Петров");
        request.setMiddleName("Петрович");

        when(userRepository.findByEmail("ivan@test.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = userService.updateProfile("ivan@test.com", request);

        assertThat(result.getFirstName()).isEqualTo("Пётр");
        assertThat(result.getLastName()).isEqualTo("Петров");
        assertThat(result.getMiddleName()).isEqualTo("Петрович");
        verify(userRepository).save(user);
    }

    // ─── findByEmail / findById ───────────────────────────────────────────────────

    @Test
    @DisplayName("findByEmail: бросает EmailNotFoundException если пользователь не найден")
    void findByEmail_throwsIfNotFound() {
        when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findByEmail("unknown@test.com"))
                .isInstanceOf(EmailNotFoundException.class);
    }

    @Test
    @DisplayName("findById: бросает UserNotFoundException если пользователь не найден")
    void findById_throwsIfNotFound() {
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findById(999L))
                .isInstanceOf(UserNotFoundException.class);
    }
}
