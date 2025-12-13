package com.project.familytree.tree.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

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