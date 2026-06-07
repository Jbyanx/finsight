package dev.jbyanx.finsight.infrastructure.security;

import dev.jbyanx.finsight.domain.auth.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final TokenRepository tokenRepository;

    // Cada día a medianoche
    @Scheduled(cron = "${scheduler.token-cleanup.cron:0 0 0 * * *}")
    @Transactional
    public void cleanExpiredAndRevokedTokens() {
        log.info("Running token cleanup at {}", LocalDateTime.now());
        tokenRepository.deleteExpiredOrRevokedTokens(LocalDateTime.now());
        log.info("Token cleanup completed");
    }
}