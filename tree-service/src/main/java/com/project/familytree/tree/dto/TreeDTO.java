package com.project.familytree.tree.dto;

import com.project.familytree.tree.impls.TreeRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public class TreeDTO {
    @Schema(description = "ID дерева")
    private Long id;

    @Schema(description = "Имя дерева")
    private String name;

    @Schema(description = "Дата создания")
    private Instant createdAt;

    @Schema(description = "Роль текущего пользователя в дереве")
    private TreeRole role;

    @Schema(description = "Токен публичной ссылки (null если не активна)")
    private String publicLinkToken;

    @Schema(description = "Количество персон в дереве")
    private long personCount;

    public TreeDTO() {
    }

    public TreeDTO(Long id, String name, Instant createdAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
    }

    public TreeDTO(Long id, String name, Instant createdAt, TreeRole role) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.role = role;
    }

    public TreeDTO(Long id, String name, Instant createdAt, TreeRole role, String publicLinkToken) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.role = role;
        this.publicLinkToken = publicLinkToken;
    }

    public TreeDTO(Long id, String name, Instant createdAt, TreeRole role, String publicLinkToken, long personCount) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.role = role;
        this.publicLinkToken = publicLinkToken;
        this.personCount = personCount;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public TreeRole getRole() { return role; }
    public void setRole(TreeRole role) { this.role = role; }

    public String getPublicLinkToken() { return publicLinkToken; }
    public void setPublicLinkToken(String publicLinkToken) { this.publicLinkToken = publicLinkToken; }

    public long getPersonCount() { return personCount; }
    public void setPersonCount(long personCount) { this.personCount = personCount; }
}