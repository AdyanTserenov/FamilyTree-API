package com.project.familytree.repositories;

import com.project.familytree.models.Invitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InvitationRepository extends JpaRepository<Invitation, Long> {
    Optional<Invitation> findByToken(String token);
    void deleteByTreeId(Long treeId);
}
