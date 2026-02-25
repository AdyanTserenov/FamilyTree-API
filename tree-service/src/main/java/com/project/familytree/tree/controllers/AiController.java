package com.project.familytree.tree.controllers;

import com.project.familytree.auth.dto.CustomApiResponse;
import com.project.familytree.auth.services.UserService;
import com.project.familytree.tree.dto.AiRequest;
import com.project.familytree.tree.dto.AiResponse;
import com.project.familytree.tree.services.AiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
@Tag(name = "AI Controller", description = "API для AI-обработки биографий с помощью Yandex GPT")
public class AiController {

    private static final Logger log = LoggerFactory.getLogger(AiController.class);

    private final AiService aiService;
    private final UserService userService;

    public AiController(AiService aiService, UserService userService) {
        this.aiService = aiService;
        this.userService = userService;
    }

    @PostMapping("/extract-facts")
    @Operation(summary = "Извлечь факты из биографии",
               description = "Анализирует текст биографии с помощью Yandex GPT и извлекает " +
                             "структурированные факты: даты, места, профессии, события. " +
                             "Лимит: 10 запросов в минуту на пользователя. " +
                             "При ошибке API возвращает пустой результат (не ошибку).")
    public ResponseEntity<CustomApiResponse<AiResponse>> extractFacts(
            @Valid @RequestBody AiRequest request) {

        UserDetails userDetails = (UserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long userId = userService.findIdByDetails(userDetails);
        log.info("AI extract-facts request from user {}, biography length: {}",
                userId, request.getBiography().length());

        AiResponse result = aiService.extractFacts(request.getBiography(), userId);
        log.info("AI extraction completed for user {}, success: {}", userId, result.isSuccess());

        return ResponseEntity.ok(CustomApiResponse.successData(result));
    }
}
