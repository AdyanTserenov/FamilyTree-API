package com.project.familytree.tree.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.familytree.tree.dto.AiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Сервис AI-интеграции для извлечения фактов из биографий.
 * Использует Yandex AI Assistant API (threads/messages/runs).
 * Промпт (system instruction) управляется из Yandex AI Studio.
 * Rate limiting: 10 запросов в минуту на пользователя.
 */
@Service
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);
    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    /** Максимальное время ожидания ответа от агента (мс) */
    private static final long RUN_TIMEOUT_MS = 30_000L;
    /** Интервал поллинга статуса run (мс) */
    private static final long POLL_INTERVAL_MS = 1_000L;

    @Value("${ai.yandex.api-key:}")
    private String apiKey;

    @Value("${ai.yandex.folder-id:}")
    private String folderId;

    @Value("${ai.yandex.assistant-id:}")
    private String assistantId;

    @Value("${ai.yandex.assistant-base-url:https://rest-assistant.api.cloud.yandex.net/assistants/v1}")
    private String baseUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /** Счётчики запросов: userId -> (count, windowStartMs) */
    private final ConcurrentHashMap<Long, long[]> rateLimitMap = new ConcurrentHashMap<>();

    public AiService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Извлечь факты из биографии с помощью Yandex AI Assistant.
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

        if (assistantId == null || assistantId.isBlank()) {
            log.warn("Yandex AI Assistant ID not configured");
            return AiResponse.empty();
        }

        try {
            return callAssistant(biography);
        } catch (Exception e) {
            log.error("AI extraction failed: {}", e.getMessage(), e);
            return AiResponse.error("Сервис AI временно недоступен. Попробуйте позже.");
        }
    }

    // ─── Yandex AI Assistant API ──────────────────────────────────────────────────

    private AiResponse callAssistant(String biography) throws Exception {
        // 1. Создать тред
        String threadId = createThread();
        log.debug("Created thread: {}", threadId);

        // 2. Добавить сообщение пользователя в тред
        addMessage(threadId, biography);
        log.debug("Added message to thread: {}", threadId);

        // 3. Запустить агента
        String runId = createRun(threadId);
        log.debug("Created run: {}", runId);

        // 4. Ждать завершения run
        waitForRun(runId);
        log.debug("Run completed: {}", runId);

        // 5. Получить последнее сообщение ассистента
        String responseText = getLastAssistantMessage(threadId);
        log.debug("Assistant response: {}", responseText);

        // 6. Распарсить JSON из ответа
        return parseAssistantResponse(responseText);
    }

    /** Создаёт новый тред. Возвращает threadId. */
    private String createThread() throws Exception {
        String url = baseUrl + "/threads";
        Map<String, Object> body = new HashMap<>();
        // folderId обязателен для Yandex AI Assistant API
        if (folderId != null && !folderId.isBlank()) {
            body.put("folderId", folderId);
        }
        ResponseEntity<String> response = restTemplate.postForEntity(url, buildEntity(body), String.class);
        JsonNode root = objectMapper.readTree(response.getBody());
        String id = root.path("id").asText();
        if (id.isBlank()) {
            throw new RuntimeException("Failed to create thread: " + response.getBody());
        }
        return id;
    }

    /** Добавляет сообщение пользователя в тред. */
    private void addMessage(String threadId, String text) throws Exception {
        String url = baseUrl + "/messages";
        Map<String, Object> body = new HashMap<>();
        body.put("threadId", threadId);
        body.put("role", "USER");

        Map<String, Object> content = new HashMap<>();
        content.put("type", "TEXT");
        Map<String, String> textContent = new HashMap<>();
        textContent.put("content", text);
        content.put("text", textContent);
        body.put("content", content);

        restTemplate.postForEntity(url, buildEntity(body), String.class);
    }

    /** Запускает агента на треде. Возвращает runId. */
    private String createRun(String threadId) throws Exception {
        String url = baseUrl + "/runs";
        Map<String, Object> body = new HashMap<>();
        body.put("assistantId", assistantId);
        body.put("threadId", threadId);

        ResponseEntity<String> response = restTemplate.postForEntity(url, buildEntity(body), String.class);
        JsonNode root = objectMapper.readTree(response.getBody());
        String id = root.path("id").asText();
        if (id.isBlank()) {
            throw new RuntimeException("Failed to create run: " + response.getBody());
        }
        return id;
    }

    /**
     * Поллит статус run до завершения (DONE) или ошибки.
     * Таймаут: {@value #RUN_TIMEOUT_MS} мс.
     */
    private void waitForRun(String runId) throws Exception {
        String url = baseUrl + "/runs/" + runId;
        long deadline = System.currentTimeMillis() + RUN_TIMEOUT_MS;

        while (System.currentTimeMillis() < deadline) {
            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, buildEntity(null), String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            String state = root.path("state").path("status").asText();
            log.debug("Run {} state: {}", runId, state);

            switch (state) {
                case "DONE":
                    return;
                case "FAILED":
                case "CANCELLED":
                    String error = root.path("state").path("error").path("message").asText("Unknown error");
                    throw new RuntimeException("Run " + runId + " failed: " + error);
                default:
                    // PENDING, IN_PROGRESS — продолжаем ждать
                    Thread.sleep(POLL_INTERVAL_MS);
            }
        }
        throw new RuntimeException("Run " + runId + " timed out after " + RUN_TIMEOUT_MS + "ms");
    }

    /** Получает последнее сообщение ассистента из треда. */
    private String getLastAssistantMessage(String threadId) throws Exception {
        String url = baseUrl + "/messages?threadId=" + threadId;
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, buildEntity(null), String.class);
        JsonNode root = objectMapper.readTree(response.getBody());
        JsonNode messages = root.path("messages");

        // Ищем последнее сообщение с role=ASSISTANT
        String lastText = null;
        if (messages.isArray()) {
            for (JsonNode msg : messages) {
                String role = msg.path("role").asText();
                if ("ASSISTANT".equalsIgnoreCase(role)) {
                    // content.text.content
                    JsonNode textNode = msg.path("content").path("text").path("content");
                    if (!textNode.isMissingNode()) {
                        lastText = textNode.asText();
                    }
                }
            }
        }

        if (lastText == null || lastText.isBlank()) {
            throw new RuntimeException("No assistant message found in thread " + threadId);
        }
        return lastText;
    }

    // ─── Парсинг ответа ───────────────────────────────────────────────────────────

    /**
     * Парсит JSON из текстового ответа агента.
     * Агент должен вернуть JSON (настраивается в инструкции в AI Studio).
     * Для надёжности ищем первый { и последний } на случай markdown-обёртки.
     */
    private AiResponse parseAssistantResponse(String text) {
        try {
            // Попытка 1: прямой парсинг (если агент вернул чистый JSON)
            return parseJsonToAiResponse(objectMapper.readTree(text));
        } catch (Exception ignored) {
            // Попытка 2: извлечь JSON из текста (если есть markdown или пояснения)
            int jsonStart = text.indexOf('{');
            int jsonEnd = text.lastIndexOf('}');
            if (jsonStart == -1 || jsonEnd == -1 || jsonEnd <= jsonStart) {
                log.warn("No JSON found in assistant response: {}", text);
                return AiResponse.empty();
            }
            try {
                String jsonText = text.substring(jsonStart, jsonEnd + 1);
                return parseJsonToAiResponse(objectMapper.readTree(jsonText));
            } catch (Exception e) {
                log.warn("Failed to parse assistant response as JSON: {}", e.getMessage());
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
