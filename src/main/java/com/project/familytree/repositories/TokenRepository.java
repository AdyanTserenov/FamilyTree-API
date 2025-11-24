package com.project.familytree.repositories;

import com.project.familytree.models.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {
    @Query("select t from Token t where t.details.tokenHash = :hash")
    Optional<Token> findByTokenHash(@Param("hash") String hash);

    @Query("SELECT t FROM Token t WHERE t.details.userId = :userId AND t.details.consumed = false")
    List<Token> findActiveTokensByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("delete from Token t where t.details.expiresAt < current_timestamp")
    int deleteExpired();
}
