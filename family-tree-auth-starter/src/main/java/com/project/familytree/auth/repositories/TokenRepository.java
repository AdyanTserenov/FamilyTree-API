package com.project.familytree.auth.repositories;

import com.project.familytree.auth.models.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {

    Optional<Token> findByTokenHash(String tokenHash);

    @Modifying
    @Query("DELETE FROM Token t WHERE t.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}