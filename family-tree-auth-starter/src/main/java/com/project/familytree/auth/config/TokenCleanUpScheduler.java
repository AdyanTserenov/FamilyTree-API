package com.project.familytree.auth.config;

import com.project.familytree.auth.repositories.TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class TokenCleanUpScheduler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TokenCleanUpScheduler.class);

    private final TokenRepository tokenRepository;

    public TokenCleanUpScheduler(TokenRepository tokenRepository) {
        this.tokenRepository = tokenRepository;
    }

    @Scheduled(cron = "0 0 3 * * ?") // Каждый день в 3:00
    public void cleanupExpiredTokens() {
        try {
            int deletedCount = tokenRepository.deleteExpired(LocalDateTime.now());
            if (deletedCount > 0) {
                log.info("Cleaned up {} expired tokens", deletedCount);
            } else {
                log.debug("No expired tokens to clean up");
            }
        } catch (Exception e) {
            log.error("Error cleaning up expired tokens", e);
        }
    }
}