package com.project.familytree.repositories;

import com.project.familytree.models.Tree;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TreeRepository extends JpaRepository<Tree, Long> {

}
