package com.project.familytree.tree.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TreeRequest {
    @Schema(description = "Имя дерева")
    @NotBlank(message = "Имя обязательно")
    private String name;

    public TreeRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}