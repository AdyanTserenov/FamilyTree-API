package com.project.familytree.tree.dto;

import com.project.familytree.tree.impls.RelationshipType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Связь между персонами")
public class RelationshipDTO {

    @Schema(description = "ID связи")
    private Long id;

    @Schema(description = "ID первой персоны (для PARENT_CHILD — родитель, для PARTNERSHIP — первый партнёр)")
    private Long person1Id;

    @Schema(description = "ID второй персоны (для PARENT_CHILD — ребёнок, для PARTNERSHIP — второй партнёр)")
    private Long person2Id;

    @Schema(description = "Тип связи: PARENT_CHILD или PARTNERSHIP")
    private RelationshipType type;

    @Schema(description = "Краткие данные первой персоны")
    private PersonSummary person1;

    @Schema(description = "Краткие данные второй персоны")
    private PersonSummary person2;

    @Schema(description = "Дата начала (только для PARTNERSHIP)")
    private LocalDate startDate;

    @Schema(description = "Дата окончания (только для PARTNERSHIP)")
    private LocalDate endDate;

    /** Краткое представление персоны для отображения в связях */
    public static class PersonSummary {
        private Long id;
        private String firstName;
        private String lastName;

        public PersonSummary() {
        }

        public PersonSummary(Long id, String firstName, String lastName) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
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
    }

    public RelationshipDTO() {
    }

    public RelationshipDTO(Long id, Long person1Id, Long person2Id, RelationshipType type) {
        this.id = id;
        this.person1Id = person1Id;
        this.person2Id = person2Id;
        this.type = type;
    }

    public RelationshipDTO(Long id, Long person1Id, Long person2Id, RelationshipType type,
                           PersonSummary person1, PersonSummary person2) {
        this.id = id;
        this.person1Id = person1Id;
        this.person2Id = person2Id;
        this.type = type;
        this.person1 = person1;
        this.person2 = person2;
    }

    public RelationshipDTO(Long id, Long person1Id, Long person2Id, RelationshipType type,
                           PersonSummary person1, PersonSummary person2,
                           LocalDate startDate, LocalDate endDate) {
        this.id = id;
        this.person1Id = person1Id;
        this.person2Id = person2Id;
        this.type = type;
        this.person1 = person1;
        this.person2 = person2;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPerson1Id() {
        return person1Id;
    }

    public void setPerson1Id(Long person1Id) {
        this.person1Id = person1Id;
    }

    public Long getPerson2Id() {
        return person2Id;
    }

    public void setPerson2Id(Long person2Id) {
        this.person2Id = person2Id;
    }

    public RelationshipType getType() {
        return type;
    }

    public void setType(RelationshipType type) {
        this.type = type;
    }

    public PersonSummary getPerson1() {
        return person1;
    }

    public void setPerson1(PersonSummary person1) {
        this.person1 = person1;
    }

    public PersonSummary getPerson2() {
        return person2;
    }

    public void setPerson2(PersonSummary person2) {
        this.person2 = person2;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }
}
