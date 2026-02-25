package com.project.familytree.tree.repositories;

import com.project.familytree.tree.models.Person;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {

    List<Person> findByTreeId(Long treeId);

    @Query("SELECT p FROM Person p WHERE p.tree.id = :treeId ORDER BY p.lastName, p.firstName")
    List<Person> findByTreeIdOrderByLastNameAscFirstNameAsc(@Param("treeId") Long treeId);

    /**
     * Поиск персон по имени, фамилии или отчеству (регистронезависимый)
     */
    @Query("SELECT p FROM Person p WHERE p.tree.id = :treeId AND (" +
           "LOWER(p.firstName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(p.lastName) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(p.middleName) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<Person> searchByName(@Param("treeId") Long treeId, @Param("q") String q);
}