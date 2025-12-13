package com.project.familytree.configurators;

import com.project.familytree.repositories.TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanUpScheduler {

    private final TokenRepository tokenRepository;

    @Scheduled(cron = "0 0 3 * * ?") // Каждый день в 3:00
    public void cleanupExpiredTokens() {
        try {
            int deletedCount = tokenRepository.deleteExpired();
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