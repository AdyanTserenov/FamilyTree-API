package com.project.familytree.tree.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Конфигурация HTTP-клиента для внешних API (Yandex GPT и др.)
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    /**
     * ObjectMapper with JavaTimeModule registered so that java.time.Instant
     * fields in DTOs (TreeDTO, CommentDTO, NotificationDTO, etc.) are serialized
     * as ISO-8601 strings instead of throwing InvalidDefinitionException.
     *
     * NOTE: This bean overrides Spring Boot's auto-configured ObjectMapper,
     * so we must explicitly register JavaTimeModule here.
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
