package com.project.familytree.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TreeRequest {
    @Schema(description = "Имя дерева")
    @NotBlank(message = "Имя обязательно")
    private String name;
}
