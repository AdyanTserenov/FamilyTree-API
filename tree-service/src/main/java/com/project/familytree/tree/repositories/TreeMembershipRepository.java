package com.project.familytree.tree.repositories;

import com.project.familytree.tree.models.TreeMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TreeMembershipRepository extends JpaRepository<TreeMembership, Long> {
    Optional<TreeMembership> findByTreeIdAndUserId(Long treeId, Long userId);

    @Query("SELECT tm FROM TreeMembership tm JOIN FETCH tm.user WHERE tm.tree.id = :treeId")
    List<TreeMembership> findByTreeId(@Param("treeId") Long treeId);

    @Query("SELECT tm FROM TreeMembership tm JOIN FETCH tm.tree WHERE tm.user.id = :userId")
    List<TreeMembership> findByUserId(@Param("userId") Long userId);
}