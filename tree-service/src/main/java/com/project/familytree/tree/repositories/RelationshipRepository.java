package com.project.familytree.tree.repositories;

import com.project.familytree.tree.impls.RelationshipType;
import com.project.familytree.tree.models.Relationship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RelationshipRepository extends JpaRepository<Relationship, Long> {

    List<Relationship> findByTreeId(Long treeId);

    List<Relationship> findByTreeIdAndType(Long treeId, RelationshipType type);

    /**
     * Все связи, в которых участвует данная персона (как person1 или person2)
     */
    @Query("SELECT r FROM Relationship r WHERE r.tree.id = :treeId AND (r.person1.id = :personId OR r.person2.id = :personId)")
    List<Relationship> findByTreeIdAndPersonId(@Param("treeId") Long treeId, @Param("personId") Long personId);

    /**
     * Все связи персоны определённого типа
     */
    @Query("SELECT r FROM Relationship r WHERE r.tree.id = :treeId AND r.type = :type AND (r.person1.id = :personId OR r.person2.id = :personId)")
    List<Relationship> findByTreeIdAndPersonIdAndType(@Param("treeId") Long treeId,
                                                      @Param("personId") Long personId,
                                                      @Param("type") RelationshipType type);

    /**
     * Поиск конкретной связи между двумя персонами определённого типа
     */
    @Query("SELECT r FROM Relationship r WHERE r.tree.id = :treeId AND r.type = :type AND " +
           "((r.person1.id = :person1Id AND r.person2.id = :person2Id) OR " +
           "(r.type = 'PARTNERSHIP' AND r.person1.id = :person2Id AND r.person2.id = :person1Id))")
    Optional<Relationship> findByTreeIdAndPersonsAndType(@Param("treeId") Long treeId,
                                                         @Param("person1Id") Long person1Id,
                                                         @Param("person2Id") Long person2Id,
                                                         @Param("type") RelationshipType type);

    /**
     * Удалить все связи персоны при её удалении
     */
    @Query("DELETE FROM Relationship r WHERE r.person1.id = :personId OR r.person2.id = :personId")
    void deleteAllByPersonId(@Param("personId") Long personId);
}
