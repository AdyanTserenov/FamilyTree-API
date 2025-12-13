package com.project.familytree.auth.services;

import com.project.familytree.auth.exceptions.InvalidTokenException;
import com.project.familytree.auth.exceptions.TokenAlreadyUsedException;
import com.project.familytree.auth.exceptions.TokenExpiredException;
import com.project.familytree.auth.exceptions.TokenNotFoundException;
import com.project.familytree.auth.impls.TokenDetails;
import com.project.familytree.auth.impls.TokenType;
import com.project.familytree.auth.models.ResetToken;
import com.project.familytree.auth.models.Token;
import com.project.familytree.auth.models.User;
import com.project.familytree.auth.models.VerifyToken;
import com.project.familytree.auth.repositories.TokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    private final TokenRepository tokenRepository;

    public TokenService(TokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @Value("${testing.app.secret:defaultSecret}")
    private String secret;

    @Value("${testing.app.lifetime:600000}")
    private long lifetime;

    public TokenDetails createVerifyToken(User user) {
        String tokenValue = UUID.randomUUID().toString();
        String tokenHash = hashToken(tokenValue);

        Token token = new Token();
        token.setType(TokenType.VERIFY);
        token.setTokenHash(tokenHash);
        token.setUser(user);
        token.setIssuedAt(LocalDateTime.now());
        token.setExpiresAt(LocalDateTime.now().plusSeconds(lifetime / 1000));
        token.setConsumed(false);

        tokenRepository.save(token);

        return new TokenDetails(tokenValue, token.getExpiresAt());
    }

    public TokenDetails createResetToken(User user) {
        String tokenValue = UUID.randomUUID().toString();
        String tokenHash = hashToken(tokenValue);

        Token token = new Token();
        token.setType(TokenType.RESET);
        token.setTokenHash(tokenHash);
        token.setUser(user);
        token.setIssuedAt(LocalDateTime.now());
        token.setExpiresAt(LocalDateTime.now().plusSeconds(lifetime / 1000));
        token.setConsumed(false);

        tokenRepository.save(token);

        return new TokenDetails(tokenValue, token.getExpiresAt());
    }

    public VerifyToken consumeVerifyToken(String tokenValue) {
        String tokenHash = hashToken(tokenValue);
        Token token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new TokenNotFoundException("Token not found"));

        if (token.getConsumed()) {
            throw new TokenAlreadyUsedException("Token already used");
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new TokenExpiredException("Token expired");
        }

        if (token.getType() != TokenType.VERIFY) {
            throw new InvalidTokenException("Invalid token type");
        }

        token.setConsumed(true);
        tokenRepository.save(token);

        return new VerifyToken(token.getUser().getId());
    }

    public ResetToken consumeResetToken(String tokenValue) {
        String tokenHash = hashToken(tokenValue);
        Token token = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new TokenNotFoundException("Token not found"));

        if (token.getConsumed()) {
            throw new TokenAlreadyUsedException("Token already used");
        }

        if (token.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new TokenExpiredException("Token expired");
        }

        if (token.getType() != TokenType.RESET) {
            throw new InvalidTokenException("Invalid token type");
        }

        token.setConsumed(true);
        tokenRepository.save(token);

        return new ResetToken(token.getUser().getId());
    }

    private String hashToken(String token) {
        // Simple hash for demo; in production use proper hashing
        return String.valueOf(token.hashCode());
    }
}