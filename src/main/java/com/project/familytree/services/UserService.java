package com.project.familytree.services;

import com.project.familytree.dto.SignUpRequest;
import com.project.familytree.exceptions.EmailNotFound;
import com.project.familytree.exceptions.InvalidRequestException;
import com.project.familytree.exceptions.UserNotFoundException;
import com.project.familytree.impls.TokenType;
import com.project.familytree.impls.UserDetailsImpl;
import com.project.familytree.models.User;
import com.project.familytree.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    public UserDetails loadUserByUsername(String email) throws EmailNotFound {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EmailNotFound("Почта не найдена: " + email));
        return UserDetailsImpl.build(user);
    }

    public User findByEmail(String email) throws EmailNotFound {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new EmailNotFound("Почта не найдена: " + email));
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
        user.setEnabled(false);

        userRepository.save(user);
        sendVerifyToken(user.getEmail());
    }

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Пользователь с таким ID не существует: " + id));
    }

    public void sendVerifyToken(String email) {
        try {
            User user = findByEmail(email);
            String token = tokenService.createVerifyToken(user.getId());

            mailSenderService.sendCreationEmail(email, token, user.getFirstName());
            log.info("Creation token created for user: {}", user.getEmail());
        } catch (EmailNotFound e) {
            log.info("User with email not found: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.info("Failed to create verify token");
            throw new RuntimeException(e.getMessage());
        }
    }

    public void sendResetToken(String email) {
        try {
            User user = findByEmail(email);
            String token = tokenService.createResetToken(user.getId());

            mailSenderService.sendPasswordResetEmail(email, token, user.getFirstName());
            log.info("Reset token created for user: {}", user.getEmail());
        } catch (EmailNotFound e) {
            log.info("User with email not found: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.info("Failed to create verify token");
            throw new RuntimeException(e.getMessage());
        }
    }

    public void confirmUser(String token) {
        Long userId = tokenService.validateToken(token, TokenType.VERIFY);
        try {
            User user = findById(userId);
            user.setEnabled(true);

            userRepository.save(user);
            tokenService.consumeToken(token);
            log.info("Successfully confirmed registration for user: {}", user.getEmail());
        } catch (UserNotFoundException e) {
            log.info("User not found: {}", e.getMessage());
            throw e;
        }
    }

    public void resetPassword(String token, String newPassword) {
        Long userId = tokenService.validateToken(token, TokenType.RESET);
        try {
            User user = findById(userId);
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

            tokenService.consumeToken(token);
            log.info("Password successfully reset for user: {}", user.getEmail());
        } catch (UserNotFoundException e) {
            log.info("User not found: {}", e.getMessage());
            throw e;
        }
    }
}
