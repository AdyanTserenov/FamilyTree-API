package com.project.familytree.tree.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public class PersonRelationshipRequest {
    @Schema(description = "ID родителя")
    @NotNull(message = "ID родителя обязателен")
    private Long parentId;

    @Schema(description = "ID ребенка")
    @NotNull(message = "ID ребенка обязателен")
    private Long childId;

    public PersonRelationshipRequest() {
    }

    public PersonRelationshipRequest(Long parentId, Long childId) {
        this.parentId = parentId;
        this.childId = childId;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public Long getChildId() {
        return childId;
    }

    public void setChildId(Long childId) {
        this.childId = childId;
    }
}