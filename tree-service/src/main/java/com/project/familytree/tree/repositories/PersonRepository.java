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

    /**
     * Расширенный поиск персон с фильтрацией по имени, году рождения и месту рождения
     */
    @Query("SELECT p FROM Person p WHERE p.tree.id = :treeId " +
           "AND (:query IS NULL OR LOWER(p.firstName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "     OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "     OR LOWER(p.middleName) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:birthYearFrom IS NULL OR YEAR(p.birthDate) >= :birthYearFrom) " +
           "AND (:birthYearTo IS NULL OR YEAR(p.birthDate) <= :birthYearTo) " +
           "AND (:birthPlace IS NULL OR LOWER(p.birthPlace) LIKE LOWER(CONCAT('%', :birthPlace, '%')))")
    List<Person> searchPersons(@Param("treeId") Long treeId,
                               @Param("query") String query,
                               @Param("birthYearFrom") Integer birthYearFrom,
                               @Param("birthYearTo") Integer birthYearTo,
                               @Param("birthPlace") String birthPlace);

    /**
     * Персоны дерева у которых есть хотя бы один медиафайл
     */
    @Query("SELECT DISTINCT p FROM Person p WHERE p.tree.id = :treeId " +
           "AND EXISTS (SELECT m FROM MediaFile m WHERE m.personId = p.id)")
    List<Person> findByTreeIdWithMedia(@Param("treeId") Long treeId);

    /**
     * Bulk count of persons grouped by treeId — used to populate personCount in TreeDTO
     * without N+1 queries.
     */
    @Query("SELECT p.tree.id, COUNT(p) FROM Person p WHERE p.tree.id IN :treeIds GROUP BY p.tree.id")
    List<Object[]> countByTreeIds(@Param("treeIds") List<Long> treeIds);
}