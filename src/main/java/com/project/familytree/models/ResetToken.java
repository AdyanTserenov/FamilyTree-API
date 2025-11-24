package com.project.familytree.models;

import com.project.familytree.impls.TokenDetails;
import com.project.familytree.impls.TokenType;
import com.project.familytree.utils.TokenGenerator;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.time.Duration;
import java.time.Instant;

import lombok.NoArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;



@Entity
@Getter
@DiscriminatorValue("RESET")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ResetToken extends Token{
    @Transient
    private String rawToken;

    public ResetToken(Long userId, Duration ttl) {
        this.rawToken = TokenGenerator.urlSafe(22);
        String hash  = DigestUtils.sha256Hex(rawToken);
        this.details = TokenDetails.of(hash, userId,
                Instant.now(), Instant.now().plus(ttl),
                false);
    }

    @Override
    public TokenType getType() {
        return TokenType.RESET;
    }
}
