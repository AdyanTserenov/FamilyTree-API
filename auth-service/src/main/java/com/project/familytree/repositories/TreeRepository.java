package com.project.familytree.repositories;

import com.project.familytree.models.Tree;
import com.project.familytree.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TreeRepository extends JpaRepository<Tree, Long> {
    Optional<Tree> findById(Long treeId);
}
