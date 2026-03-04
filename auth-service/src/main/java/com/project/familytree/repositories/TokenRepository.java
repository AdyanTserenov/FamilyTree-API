package com.project.familytree.repositories;

import com.project.familytree.models.ResetToken;
import com.project.familytree.models.Token;
import com.project.familytree.models.VerifyToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {
    @Query("select t from Token t where t.details.tokenHash = :hash")
    Optional<Token> findByTokenHash(@Param("hash") String hash);

    @Query("SELECT t FROM Token t WHERE t.details.userId = :userId AND t.details.consumed = false")
    List<Token> findActiveTokensByUserId(@Param("userId") Long userId);

    @Query("SELECT t FROM VerifyToken t WHERE t.details.userId = :userId AND t.details.consumed = false")
    List<VerifyToken> findActiveVerifyTokensByUserId(@Param("userId") Long userId);

    @Query("SELECT t FROM ResetToken t WHERE t.details.userId = :userId AND t.details.consumed = false")
    List<ResetToken> findActiveResetTokensByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("delete from Token t where t.details.expiresAt < current_timestamp")
    int deleteExpired();

    @Modifying
    @Query("delete from Token t where t.details.userId = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);
}
