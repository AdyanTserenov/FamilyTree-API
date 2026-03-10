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
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис AI-интеграции для извлечения фактов из биографий.
 * Использует Yandex GPT Completion API (YandexGPT Pro).
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
     *
     * @param biography текст биографии
     * @param userId    ID пользователя (для rate limiting)
     * @return структурированные факты
     */
    public AiResponse extractFacts(String biography, Long userId) {
        if (!checkRateLimit(userId)) {
            log.warn("Rate limit exceeded for user {}", userId);
            return AiResponse.error("Превышен лимит запросов. Попробуйте через минуту.");
        }

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("Yandex AI API key not configured");
            return AiResponse.empty();
        }

        if (folderId == null || folderId.isBlank()) {
            log.warn("Yandex AI folder ID not configured");
            return AiResponse.empty();
        }

        try {
            return callYandexGpt(biography);
        } catch (Exception e) {
            log.error("AI extraction failed: {}", e.getMessage(), e);
            return AiResponse.error("Сервис AI временно недоступен. Попробуйте позже.");
        }
    }

    // ─── Yandex GPT Completion API ────────────────────────────────────────────────

    private AiResponse callYandexGpt(String biography) throws Exception {
        Map<String, Object> requestBody = buildRequest(biography);
        HttpEntity<Map<String, Object>> entity = buildEntity(requestBody);

        String responseBody = restTemplate.postForObject(apiUrl, entity, String.class);
        return parseResponse(responseBody);
    }

    private Map<String, Object> buildRequest(String biography) {
        Map<String, Object> request = new HashMap<>();
        request.put("modelUri", "gpt://" + folderId + "/yandexgpt/latest");

        Map<String, Object> completionOptions = new HashMap<>();
        completionOptions.put("stream", false);
        completionOptions.put("temperature", 0.1);
        completionOptions.put("maxTokens", 2000);
        request.put("completionOptions", completionOptions);

        List<Map<String, String>> messages = new ArrayList<>();

        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("text", buildSystemPrompt());
        messages.add(systemMessage);

        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("text", biography);
        messages.add(userMessage);

        request.put("messages", messages);
        return request;
    }

    private String buildSystemPrompt() {
        return """
                Ты — помощник для анализа биографий. Извлеки из текста биографии следующие факты и верни их строго в формате JSON без каких-либо пояснений.
                
                Формат ответа (только JSON, без markdown, без пояснений):
                {
                  "dates": ["список дат и временных периодов, упомянутых в тексте"],
                  "places": ["список географических мест: городов, стран, регионов"],
                  "professions": ["список профессий, должностей, специальностей"],
                  "events": ["список ключевых событий жизни"],
                  "summary": "краткое резюме биографии в 2-3 предложениях"
                }
                
                Если какой-то тип данных не найден, верни пустой массив []. Отвечай только JSON.
                """;
    }

    private AiResponse parseResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode alternatives = root.path("result").path("alternatives");

        if (!alternatives.isArray() || alternatives.isEmpty()) {
            log.warn("No alternatives in YandexGPT response: {}", responseBody);
            return AiResponse.empty();
        }

        String text = alternatives.get(0).path("message").path("text").asText("");
        if (text.isBlank()) {
            log.warn("Empty text in YandexGPT response");
            return AiResponse.empty();
        }

        log.debug("YandexGPT raw response text: {}", text);
        return parseJsonFromText(text);
    }

    /**
     * Парсит JSON из текстового ответа модели.
     * Для надёжности ищем первый { и последний } на случай markdown-обёртки.
     */
    private AiResponse parseJsonFromText(String text) {
        try {
            // Попытка 1: прямой парсинг
            return parseJsonToAiResponse(objectMapper.readTree(text));
        } catch (Exception ignored) {
            // Попытка 2: извлечь JSON из текста (если есть markdown или пояснения)
            int jsonStart = text.indexOf('{');
            int jsonEnd = text.lastIndexOf('}');
            if (jsonStart == -1 || jsonEnd == -1 || jsonEnd <= jsonStart) {
                log.warn("No JSON found in YandexGPT response: {}", text);
                return AiResponse.empty();
            }
            try {
                String jsonText = text.substring(jsonStart, jsonEnd + 1);
                return parseJsonToAiResponse(objectMapper.readTree(jsonText));
            } catch (Exception e) {
                log.warn("Failed to parse YandexGPT response as JSON: {}", e.getMessage());
                return AiResponse.empty();
            }
        }
    }

    private AiResponse parseJsonToAiResponse(JsonNode parsed) {
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

    // ─── HTTP helpers ─────────────────────────────────────────────────────────────

    private <T> HttpEntity<T> buildEntity(T body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Api-Key " + apiKey);
        return new HttpEntity<>(body, headers);
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
                return new long[]{1, now};
            }
            data[0]++;
            return data;
        });

        long[] data = rateLimitMap.get(userId);
        return data != null && data[0] <= MAX_REQUESTS_PER_MINUTE;
    }
}
