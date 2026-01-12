package com.project.familytree.tree.dto;

import com.project.familytree.tree.impls.Gender;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.Set;

public class PersonDTO {
    @Schema(description = "ID персоны")
    private Long id;

    @Schema(description = "ID дерева")
    private Long treeId;

    @Schema(description = "Имя")
    private String firstName;

    @Schema(description = "Фамилия")
    private String lastName;

    @Schema(description = "Отчество")
    private String middleName;

    @Schema(description = "Дата рождения")
    private LocalDate birthDate;

    @Schema(description = "Дата смерти")
    private LocalDate deathDate;

    @Schema(description = "Пол")
    private Gender gender;

    @Schema(description = "ID родителей")
    private Set<Long> parentIds;

    @Schema(description = "ID детей")
    private Set<Long> childIds;

    @Schema(description = "Полное имя")
    private String fullName;

    public PersonDTO() {
    }

    public PersonDTO(Long id, Long treeId, String firstName, String lastName, String middleName,
                    LocalDate birthDate, LocalDate deathDate, Gender gender,
                    Set<Long> parentIds, Set<Long> childIds, String fullName) {
        this.id = id;
        this.treeId = treeId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.middleName = middleName;
        this.birthDate = birthDate;
        this.deathDate = deathDate;
        this.gender = gender;
        this.parentIds = parentIds;
        this.childIds = childIds;
        this.fullName = fullName;
    }

    // Getters and setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTreeId() {
        return treeId;
    }

    public void setTreeId(Long treeId) {
        this.treeId = treeId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public LocalDate getDeathDate() {
        return deathDate;
    }

    public void setDeathDate(LocalDate deathDate) {
        this.deathDate = deathDate;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public Set<Long> getParentIds() {
        return parentIds;
    }

    public void setParentIds(Set<Long> parentIds) {
        this.parentIds = parentIds;
    }

    public Set<Long> getChildIds() {
        return childIds;
    }

    public void setChildIds(Set<Long> childIds) {
        this.childIds = childIds;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}