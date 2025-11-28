package com.project.familytree.repositories;

import com.project.familytree.models.TreeMembership;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TreeMembershipRepository extends JpaRepository<TreeMembership, Long> {
    Optional<TreeMembership> findByTreeIdAndUserId(Long treeId, Long userId);
    List<TreeMembership> findByTreeId(Long treeId);
    void deleteByTreeIdAndUserId(Long treeId, Long userId);
}
