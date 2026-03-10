package com.project.familytree.tree.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.familytree.tree.dto.AiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Сервис AI-интеграции для извлечения фактов из биографий.
 * Использует Yandex GPT API.
 * Rate limiting: 10 запросов в минуту на пользователя.
 */
@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    private static final int MAX_REQUESTS_PER_MINUTE = 10;

    @Value("${ai.yandex.api-key:}")
    private String apiKey;

    @Value("${ai.yandex.folder-id:}")
    private String folderId;

    @Value("${ai.yandex.api-url:https://llm.api.cloud.yandex.net/foundationModels/v1/completion}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /** Счётчики запросов: userId -> (count, windowStartMs) */
    private final ConcurrentHashMap<Long, long[]> rateLimitMap = new ConcurrentHashMap<>();

    public AiService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Извлечь факты из биографии с помощью Yandex GPT.
     * При ошибке или отсутствии API-ключа возвращает пустой результат.
     *
     * @param biography текст биографии
     * @param userId    ID пользователя (для rate limiting)
     * @return структурированные факты
     */
    public AiResponse extractFacts(String biography, Long userId) {
        // Проверка rate limit
        if (!checkRateLimit(userId)) {
            log.warn("Rate limit exceeded for user {}", userId);
            return AiResponse.error("Превышен лимит запросов. Попробуйте через минуту.");
        }

        // Если API-ключ не настроен — возвращаем пустой результат
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Yandex GPT API key not configured, returning empty result");
            return AiResponse.empty();
        }

        try {
            return callYandexGpt(biography);
        } catch (Exception e) {
            log.error("AI extraction failed: {}", e.getMessage(), e);
            return AiResponse.error("Сервис AI временно недоступен. Попробуйте позже.");
        }
    }

    // ─── Yandex GPT API call ──────────────────────────────────────────────────────

    private AiResponse callYandexGpt(String biography) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Api-Key " + apiKey);
        headers.set("x-folder-id", folderId);

        String prompt = buildPrompt(biography);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("modelUri", "gpt://" + folderId + "/yandexgpt-lite");

        Map<String, Object> completionOptions = new HashMap<>();
        completionOptions.put("stream", false);
        completionOptions.put("temperature", 0.1);
        completionOptions.put("maxTokens", 1000);
        requestBody.put("completionOptions", completionOptions);

        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        systemMsg.put("text", "Ты помощник для анализа биографий. Извлекай факты в формате JSON.");
        messages.add(systemMsg);

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("text", prompt);
        messages.add(userMsg);
        requestBody.put("messages", messages);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);

        return parseYandexGptResponse(response.getBody());
    }

    private String buildPrompt(String biography) {
        return "Проанализируй следующую биографию и извлеки факты в формате JSON:\n\n" +
               biography + "\n\n" +
               "Верни JSON в формате:\n" +
               "{\n" +
               "  \"dates\": [\"список дат и периодов\"],\n" +
               "  \"places\": [\"список мест\"],\n" +
               "  \"professions\": [\"список профессий и занятий\"],\n" +
               "  \"events\": [\"список ключевых событий\"],\n" +
               "  \"summary\": \"краткое резюме в 1-2 предложениях\"\n" +
               "}\n" +
               "Если информация отсутствует — используй пустой массив [].";
    }

    private AiResponse parseYandexGptResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode textNode = root.path("result").path("alternatives").get(0).path("message").path("text");

        if (textNode.isMissingNode()) {
            log.warn("Unexpected Yandex GPT response structure");
            return AiResponse.empty();
        }

        String text = textNode.asText();

        // Извлекаем JSON из ответа (может быть обёрнут в markdown)
        int jsonStart = text.indexOf('{');
        int jsonEnd = text.lastIndexOf('}');
        if (jsonStart == -1 || jsonEnd == -1) {
            return AiResponse.empty();
        }
        String jsonText = text.substring(jsonStart, jsonEnd + 1);

        JsonNode parsed = objectMapper.readTree(jsonText);

        List<String> dates = parseStringList(parsed.path("dates"));
        List<String> places = parseStringList(parsed.path("places"));
        List<String> professions = parseStringList(parsed.path("professions"));
        List<String> events = parseStringList(parsed.path("events"));
        String summary = parsed.path("summary").asText("");

        return new AiResponse(dates, places, professions, events, summary);
    }

    private List<String> parseStringList(JsonNode node) {
        List<String> result = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(item -> result.add(item.asText()));
        }
        return result;
    }

    // ─── Rate limiting ────────────────────────────────────────────────────────────

    /**
     * Проверяет и обновляет счётчик запросов для пользователя.
     * Окно: 1 минута (60 000 мс).
     *
     * @return true если запрос разрешён, false если лимит превышен
     */
    private boolean checkRateLimit(Long userId) {
        long now = System.currentTimeMillis();
        long windowMs = 60_000L;

        rateLimitMap.compute(userId, (id, data) -> {
            if (data == null || now - data[1] > windowMs) {
                // Новое окно
                return new long[]{1, now};
            }
            data[0]++;
            return data;
        });

        long[] data = rateLimitMap.get(userId);
        return data != null && data[0] <= MAX_REQUESTS_PER_MINUTE;
    }
}
