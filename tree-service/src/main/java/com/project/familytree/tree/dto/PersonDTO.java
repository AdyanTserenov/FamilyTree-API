package com.project.familytree.tree.dto;

import com.project.familytree.tree.impls.Gender;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

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

    @Schema(description = "Место рождения")
    private String birthPlace;

    @Schema(description = "Место смерти")
    private String deathPlace;

    @Schema(description = "Профессия / род занятий")
    private String occupation;

    @Schema(description = "Биография")
    private String biography;

    @Schema(description = "URL аватара персоны")
    private String avatarUrl;

    @Schema(description = "Пол")
    private Gender gender;

    @Schema(description = "Связи персоны (PARENT_CHILD и PARTNERSHIP)")
    private List<RelationshipDTO> relationships;

    @Schema(description = "Полное имя")
    private String fullName;

    @Schema(description = "Количество медиафайлов персоны")
    private long mediaCount;

    public PersonDTO() {
    }

    public PersonDTO(Long id, Long treeId, String firstName, String lastName, String middleName,
                     LocalDate birthDate, LocalDate deathDate,
                     String birthPlace, String deathPlace, String biography, String avatarUrl,
                     Gender gender, List<RelationshipDTO> relationships, String fullName) {
        this.id = id;
        this.treeId = treeId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.middleName = middleName;
        this.birthDate = birthDate;
        this.deathDate = deathDate;
        this.birthPlace = birthPlace;
        this.deathPlace = deathPlace;
        this.biography = biography;
        this.avatarUrl = avatarUrl;
        this.gender = gender;
        this.relationships = relationships;
        this.fullName = fullName;
    }

    public PersonDTO(Long id, Long treeId, String firstName, String lastName, String middleName,
                     LocalDate birthDate, LocalDate deathDate,
                     String birthPlace, String deathPlace, String occupation, String biography,
                     String avatarUrl, Gender gender, List<RelationshipDTO> relationships,
                     String fullName) {
        this(id, treeId, firstName, lastName, middleName, birthDate, deathDate,
                birthPlace, deathPlace, biography, avatarUrl, gender, relationships, fullName);
        this.occupation = occupation;
    }

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

    public String getBirthPlace() {
        return birthPlace;
    }

    public void setBirthPlace(String birthPlace) {
        this.birthPlace = birthPlace;
    }

    public String getDeathPlace() {
        return deathPlace;
    }

    public void setDeathPlace(String deathPlace) {
        this.deathPlace = deathPlace;
    }

    public String getOccupation() {
        return occupation;
    }

    public void setOccupation(String occupation) {
        this.occupation = occupation;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }

    public List<RelationshipDTO> getRelationships() {
        return relationships;
    }

    public void setRelationships(List<RelationshipDTO> relationships) {
        this.relationships = relationships;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public long getMediaCount() {
        return mediaCount;
    }

    public void setMediaCount(long mediaCount) {
        this.mediaCount = mediaCount;
    }
}
