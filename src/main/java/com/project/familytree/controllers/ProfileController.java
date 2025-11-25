package com.project.familytree.controllers;

import com.project.familytree.dto.CustomApiResponse;
import com.project.familytree.dto.ProfileResponse;
import com.project.familytree.exceptions.EmailNotFound;
import com.project.familytree.models.User;
import com.project.familytree.security.JwtCore;
import com.project.familytree.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/profile")
@Tag(name = "Profile controller", description = "API для личного кабинета")
public class ProfileController {
    private final JwtCore jwtCore;
    private final UserService userService;

    @GetMapping("/me")
    @Operation(
            summary = "Получить информацию о себе по JWT",
            description = "Возвращает профиль пользователя в стандартизированной обёртке ApiResponse",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Профиль пользователя",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                    {
                      "status": "success",
                      "data": {
                        "firstName": "Иван",
                        "lastName": "Иванов",
                        "middleName": "Иванович",
                        "email": "ivanov@example.com"
                      },
                      "error": null,
                      "details": null
                    }
                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Пользователь не найден",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                    {
                      "status": "error",
                      "data": null,
                      "error": "User not found",
                      "details": null
                    }
                    """
                                    )
                            )
                    )
            }
    )
        public ResponseEntity<CustomApiResponse<ProfileResponse>> getInfo(HttpServletRequest request) {
                String authorizationHeader = request.getHeader("Authorization");
    
                if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
                        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid Authorization header");
                }
    
                String token = authorizationHeader.substring(7);
                String email = jwtCore.getEmailFromJwt(token);
        try {
            User user = userService.findByEmail(email);
            ProfileResponse profileResponse = new ProfileResponse(
                    user.getFirstName(),
                    user.getLastName(),
                    user.getMiddleName(),
                    user.getEmail()
            );
            return ResponseEntity.ok(CustomApiResponse.success(profileResponse));
        } catch (EmailNotFound e) {
            log.info("User with email not found {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }
}
