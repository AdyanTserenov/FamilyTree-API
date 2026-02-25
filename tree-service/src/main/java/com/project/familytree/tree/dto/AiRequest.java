package com.project.familytree.tree.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Запрос на AI-обработку биографии персоны
 */
public class AiRequest {

    @NotBlank(message = "Биография не может быть пустой")
    @Size(max = 10000, message = "Биография не может превышать 10000 символов")
    @Schema(description = "Текст биографии для извлечения фактов",
            example = "Иван Петрович родился 15 марта 1920 года в Москве. " +
                      "Работал инженером на заводе. Скончался в 1985 году в Санкт-Петербурге.")
    private String biography;

    @Schema(description = "ID персоны (опционально, для сохранения результата)", nullable = true)
    private Long personId;

    public AiRequest() {
    }

    public AiRequest(String biography, Long personId) {
        this.biography = biography;
        this.personId = personId;
    }

    public String getBiography() { return biography; }
    public void setBiography(String biography) { this.biography = biography; }

    public Long getPersonId() { return personId; }
    public void setPersonId(Long personId) { this.personId = personId; }
}
