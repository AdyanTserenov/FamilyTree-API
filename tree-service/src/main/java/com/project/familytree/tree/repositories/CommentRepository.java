package com.project.familytree.tree.repositories;

import com.project.familytree.tree.models.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * Удалить все комментарии к персонам дерева (используется при удалении дерева)
     */
    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM Comment c WHERE c.person.id IN (SELECT p.id FROM Person p WHERE p.tree.id = :treeId)")
    void deleteByTreeId(@Param("treeId") Long treeId);

    /**
     * Все комментарии к персоне, отсортированные по дате создания (старые первые)
     */
    @Query("SELECT c FROM Comment c WHERE c.person.id = :personId ORDER BY c.createdAt ASC")
    List<Comment> findByPersonId(@Param("personId") Long personId);

    /**
     * Комментарии верхнего уровня к персоне (без родителя)
     */
    @Query("SELECT c FROM Comment c WHERE c.person.id = :personId AND c.parentComment IS NULL ORDER BY c.createdAt ASC")
    List<Comment> findTopLevelByPersonId(@Param("personId") Long personId);

    /**
     * Ответы на конкретный комментарий
     */
    @Query("SELECT c FROM Comment c WHERE c.parentComment.id = :parentId ORDER BY c.createdAt ASC")
    List<Comment> findRepliesByParentId(@Param("parentId") Long parentId);

    /**
     * Количество комментариев к персоне (включая удалённые)
     */
    long countByPersonId(Long personId);
}
