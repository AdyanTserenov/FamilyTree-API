package com.project.familytree.tree.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public class TreeDTO {
    @Schema(description = "ID дерева")
    private Long id;

    @Schema(description = "Имя дерева")
    private String name;

    @Schema(description = "Дата создания")
    private Instant createdAt;

    public TreeDTO() {
    }

    public TreeDTO(Long id, String name, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}