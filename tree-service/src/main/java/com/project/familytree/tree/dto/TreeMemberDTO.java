package com.project.familytree.tree.dto;

import com.project.familytree.tree.impls.TreeRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * DTO для участника дерева — включает роль и дату вступления.
 */
@Schema(description = "Участник семейного дерева")
public class TreeMemberDTO {

    @Schema(description = "ID пользователя")
    private Long userId;

    @Schema(description = "Имя")
    private String firstName;

    @Schema(description = "Фамилия")
    private String lastName;

    @Schema(description = "Отчество")
    private String middleName;

    @Schema(description = "Email")
    private String email;

    @Schema(description = "Роль в дереве")
    private TreeRole role;

    @Schema(description = "Дата вступления")
    private Instant joinedAt;

    public TreeMemberDTO() {
    }

    public TreeMemberDTO(Long userId, String firstName, String lastName, String middleName,
                         String email, TreeRole role, Instant joinedAt) {
        this.userId = userId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.middleName = middleName;
        this.email = email;
        this.role = role;
        this.joinedAt = joinedAt;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getMiddleName() { return middleName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public TreeRole getRole() { return role; }
    public void setRole(TreeRole role) { this.role = role; }

    public Instant getJoinedAt() { return joinedAt; }
    public void setJoinedAt(Instant joinedAt) { this.joinedAt = joinedAt; }
}
