package com.project.familytree.tree.dto;

import com.project.familytree.tree.impls.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class PersonRequest {

    @Schema(description = "Имя", example = "Иван")
    @NotBlank(message = "Имя обязательно")
    private String firstName;

    @Schema(description = "Фамилия", example = "Иванов")
    @NotBlank(message = "Фамилия обязательна")
    private String lastName;

    @Schema(description = "Отчество", example = "Иванович")
    private String middleName;

    @Schema(description = "Дата рождения", example = "1990-01-01")
    private LocalDate birthDate;

    @Schema(description = "Дата смерти", example = "2020-12-31")
    private LocalDate deathDate;

    @Schema(description = "Место рождения", example = "Москва")
    private String birthPlace;

    @Schema(description = "Место смерти", example = "Санкт-Петербург")
    private String deathPlace;

    @Schema(description = "Профессия / род занятий", example = "Инженер")
    private String occupation;

    @Schema(description = "Биография")
    private String biography;

    @Schema(description = "Пол")
    @NotNull(message = "Пол обязателен")
    private Gender gender;

    public PersonRequest() {
    }

    public PersonRequest(String firstName, String lastName, String middleName,
                         LocalDate birthDate, LocalDate deathDate,
                         String birthPlace, String deathPlace, String occupation,
                         String biography, Gender gender) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.middleName = middleName;
        this.birthDate = birthDate;
        this.deathDate = deathDate;
        this.birthPlace = birthPlace;
        this.deathPlace = deathPlace;
        this.occupation = occupation;
        this.biography = biography;
        this.gender = gender;
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

    public Gender getGender() {
        return gender;
    }

    public void setGender(Gender gender) {
        this.gender = gender;
    }
}
