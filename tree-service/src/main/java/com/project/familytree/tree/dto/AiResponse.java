package com.project.familytree.tree.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Ответ AI-сервиса с извлечёнными фактами из биографии
 */
public class AiResponse {

    @Schema(description = "Извлечённые даты (рождение, смерть, события)")
    private List<String> dates;

    @Schema(description = "Извлечённые места (рождение, проживание, смерть)")
    private List<String> places;

    @Schema(description = "Извлечённые профессии и занятия")
    private List<String> professions;

    @Schema(description = "Извлечённые события жизни")
    private List<String> events;

    @Schema(description = "Краткое резюме биографии")
    private String summary;

    @Schema(description = "Флаг успешности обработки")
    private boolean success;

    @Schema(description = "Сообщение об ошибке (если success=false)")
    private String errorMessage;

    public AiResponse() {
    }

    public AiResponse(List<String> dates, List<String> places, List<String> professions,
                      List<String> events, String summary) {
        this.dates = dates;
        this.places = places;
        this.professions = professions;
        this.events = events;
        this.summary = summary;
        this.success = true;
    }

    public static AiResponse empty() {
        AiResponse response = new AiResponse();
        response.success = true;
        response.dates = List.of();
        response.places = List.of();
        response.professions = List.of();
        response.events = List.of();
        response.summary = "";
        return response;
    }

    public static AiResponse error(String message) {
        AiResponse response = new AiResponse();
        response.success = false;
        response.errorMessage = message;
        response.dates = List.of();
        response.places = List.of();
        response.professions = List.of();
        response.events = List.of();
        return response;
    }

    public List<String> getDates() { return dates; }
    public void setDates(List<String> dates) { this.dates = dates; }

    public List<String> getPlaces() { return places; }
    public void setPlaces(List<String> places) { this.places = places; }

    public List<String> getProfessions() { return professions; }
    public void setProfessions(List<String> professions) { this.professions = professions; }

    public List<String> getEvents() { return events; }
    public void setEvents(List<String> events) { this.events = events; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
