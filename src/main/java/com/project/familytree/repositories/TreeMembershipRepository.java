package com.project.familytree.repositories;

import com.project.familytree.models.TreeMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TreeMembershipRepository extends JpaRepository<TreeMembership, Long> {
    Optional<TreeMembership> findByTreeIdAndUserId(Long treeId, Long userId);

    @Query("SELECT tm FROM TreeMembership tm JOIN FETCH tm.user WHERE tm.tree.id = :treeId")
    List<TreeMembership> findByTreeId(@Param("treeId") Long treeId);

    void deleteByTreeIdAndUserId(Long treeId, Long userId);
}
