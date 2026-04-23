package com.project.familytree.tree.repositories;

import com.project.familytree.tree.models.PersonHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PersonHistoryRepository extends JpaRepository<PersonHistory, Long> {

    List<PersonHistory> findByPersonIdAndTreeIdOrderByCreatedAtDesc(Long personId, Long treeId);

    void deleteByTreeId(Long treeId);
}
