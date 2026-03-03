package com.project.familytree.tree.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.familytree.tree.dto.AiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiServiceTest {

    @Mock private RestTemplate restTemplate;

    private AiService aiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        aiService = new AiService(restTemplate, objectMapper);
        // Inject @Value fields
        ReflectionTestUtils.setField(aiService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(aiService, "folderId", "test-folder-id");
        ReflectionTestUtils.setField(aiService, "apiUrl",
                "https://llm.api.cloud.yandex.net/foundationModels/v1/completion");
    }

    // ─── extractFacts — rate limit ────────────────────────────────────────────────

    @Test
    @DisplayName("extractFacts: возвращает ошибку при превышении rate limit (>10 запросов в минуту)")
    void extractFacts_returnsErrorWhenRateLimitExceeded() {
        // Мокируем успешный ответ Yandex GPT
        String yandexResponse = buildYandexGptResponse(
                "{\"dates\":[],\"places\":[],\"professions\":[],\"events\":[],\"summary\":\"\"}"
        );
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok(yandexResponse));

        Long userId = 999L;

        // Делаем 10 успешных запросов
        for (int i = 0; i < 10; i++) {
            AiResponse response = aiService.extractFacts("Биография " + i, userId);
            assertThat(response.isSuccess()).isTrue();
        }

        // 11-й запрос должен вернуть ошибку rate limit
        AiResponse rateLimitResponse = aiService.extractFacts("Биография 11", userId);
        assertThat(rateLimitResponse.isSuccess()).isFalse();
        assertThat(rateLimitResponse.getErrorMessage()).contains("лимит");
    }

    @Test
    @DisplayName("extractFacts: возвращает пустой результат если API-ключ не настроен")
    void extractFacts_returnsEmptyWhenApiKeyBlank() {
        ReflectionTestUtils.setField(aiService, "apiKey", "");

        AiResponse response = aiService.extractFacts("Биография", 1L);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getDates()).isEmpty();
        assertThat(response.getPlaces()).isEmpty();
        assertThat(response.getSummary()).isNullOrEmpty();
        verifyNoInteractions(restTemplate);
    }

    // ─── extractFacts — happy path ────────────────────────────────────────────────

    @Test
    @DisplayName("extractFacts: парсит ответ Yandex GPT и возвращает структурированные данные")
    void extractFacts_parsesYandexGptResponseCorrectly() {
        String jsonContent = """
                {
                  "dates": ["1990", "2010-2015"],
                  "places": ["Москва", "Санкт-Петербург"],
                  "professions": ["инженер", "программист"],
                  "events": ["окончил университет", "основал компанию"],
                  "summary": "Успешный инженер из Москвы."
                }
                """;
        String yandexResponse = buildYandexGptResponse(jsonContent);

        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok(yandexResponse));

        AiResponse response = aiService.extractFacts("Биография персоны", 1L);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getDates()).containsExactly("1990", "2010-2015");
        assertThat(response.getPlaces()).containsExactly("Москва", "Санкт-Петербург");
        assertThat(response.getProfessions()).containsExactly("инженер", "программист");
        assertThat(response.getEvents()).containsExactly("окончил университет", "основал компанию");
        assertThat(response.getSummary()).isEqualTo("Успешный инженер из Москвы.");
    }

    @Test
    @DisplayName("extractFacts: возвращает пустой результат при ошибке HTTP")
    void extractFacts_returnsEmptyOnHttpError() {
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        AiResponse response = aiService.extractFacts("Биография", 1L);

        // Не бросает исключение, возвращает пустой результат
        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getDates()).isEmpty();
    }

    @Test
    @DisplayName("extractFacts: обрабатывает JSON обёрнутый в markdown-блок")
    void extractFacts_handlesMarkdownWrappedJson() {
        String jsonContent = "```json\n{\"dates\":[\"1985\"],\"places\":[\"Казань\"],\"professions\":[],\"events\":[],\"summary\":\"Краткое резюме\"}\n```";
        String yandexResponse = buildYandexGptResponse(jsonContent);

        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok(yandexResponse));

        AiResponse response = aiService.extractFacts("Биография", 2L);

        assertThat(response.getDates()).containsExactly("1985");
        assertThat(response.getPlaces()).containsExactly("Казань");
        assertThat(response.getSummary()).isEqualTo("Краткое резюме");
    }

    @Test
    @DisplayName("extractFacts: разные пользователи имеют независимые счётчики rate limit")
    void extractFacts_differentUsersHaveIndependentRateLimits() {
        String yandexResponse = buildYandexGptResponse(
                "{\"dates\":[],\"places\":[],\"professions\":[],\"events\":[],\"summary\":\"\"}"
        );
        when(restTemplate.postForEntity(anyString(), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok(yandexResponse));

        // Исчерпываем лимит для user 100
        for (int i = 0; i < 10; i++) {
            aiService.extractFacts("bio", 100L);
        }
        AiResponse limitedForUser100 = aiService.extractFacts("bio", 100L);
        assertThat(limitedForUser100.isSuccess()).isFalse();

        // User 200 должен работать нормально
        AiResponse okForUser200 = aiService.extractFacts("bio", 200L);
        assertThat(okForUser200.isSuccess()).isTrue();
    }

    // ─── helpers ─────────────────────────────────────────────────────────────────

    /**
     * Строит JSON-ответ в формате Yandex GPT API.
     */
    private String buildYandexGptResponse(String textContent) {
        // Экранируем кавычки в textContent для вставки в JSON
        String escaped = textContent.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        return """
                {
                  "result": {
                    "alternatives": [
                      {
                        "message": {
                          "role": "assistant",
                          "text": "%s"
                        },
                        "status": "ALTERNATIVE_STATUS_FINAL"
                      }
                    ],
                    "usage": {
                      "inputTextTokens": "100",
                      "completionTokens": "200",
                      "totalTokens": "300"
                    },
                    "modelVersion": "23.10.2024"
                  }
                }
                """.formatted(escaped);
    }
}
