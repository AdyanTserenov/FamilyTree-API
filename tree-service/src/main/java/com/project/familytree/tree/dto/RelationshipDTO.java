package com.project.familytree.tree.dto;

import com.project.familytree.tree.impls.RelationshipType;
import io.swagger.v3.oas.annotations.media.Schema;

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

    public RelationshipDTO() {
    }

    public RelationshipDTO(Long id, Long person1Id, Long person2Id, RelationshipType type) {
        this.id = id;
        this.person1Id = person1Id;
        this.person2Id = person2Id;
        this.type = type;
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
}
