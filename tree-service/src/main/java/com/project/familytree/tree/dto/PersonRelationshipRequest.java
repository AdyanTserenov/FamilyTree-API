package com.project.familytree.tree.dto;

import com.project.familytree.tree.impls.RelationshipType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class PersonRelationshipRequest {

    @Schema(description = "ID первой персоны (для PARENT_CHILD — родитель, для PARTNERSHIP — первый партнёр)")
    @NotNull(message = "ID первой персоны обязателен")
    private Long person1Id;

    @Schema(description = "ID второй персоны (для PARENT_CHILD — ребёнок, для PARTNERSHIP — второй партнёр)")
    @NotNull(message = "ID второй персоны обязателен")
    private Long person2Id;

    @Schema(description = "Тип связи: PARENT_CHILD или PARTNERSHIP")
    @NotNull(message = "Тип связи обязателен")
    private RelationshipType type;

    @Schema(description = "Дата начала (только для PARTNERSHIP)")
    private LocalDate startDate;

    @Schema(description = "Дата окончания (только для PARTNERSHIP)")
    private LocalDate endDate;

    public PersonRelationshipRequest() {
    }

    public PersonRelationshipRequest(Long person1Id, Long person2Id, RelationshipType type) {
        this.person1Id = person1Id;
        this.person2Id = person2Id;
        this.type = type;
    }

    public Long getPerson1Id() {
        return person1Id;
    }

    public void setPerson1Id(Long person1Id) {
        this.person1Id = person1Id;
    }

    public Long getPerson2Id() {
        return person2Id;
    }

    public void setPerson2Id(Long person2Id) {
        this.person2Id = person2Id;
    }

    public RelationshipType getType() {
        return type;
    }

    public void setType(RelationshipType type) {
        this.type = type;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    // Backward-compat aliases for PARENT_CHILD usage
    public Long getParentId() {
        return person1Id;
    }

    public Long getChildId() {
        return person2Id;
    }
}
