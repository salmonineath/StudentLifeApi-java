package com.studentlife.studentlifejava.script;

import com.studentlife.studentlifejava.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenCleanupJob {

    private final RefreshTokenRepository refreshTokenRepository;

    // Runs daily at 03:00 in the server/container's default timezone (no explicit
    // zone set) - if this app is ever deployed across regions with different TZ
    // env vars, the actual wall-clock run time shifts with it. This is a hard
    // DELETE, not a soft-delete/archive: purged rows are unrecoverable.
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeExpiredTokens() {
        // Also removes revoked-but-not-yet-expired rows (rotated tokens, reuse-
        // detected sessions) - otherwise those sit in the table until their
        // original expiry date even though they can never be used again.
        int deleted = refreshTokenRepository.deleteAllByExpiredAtBefore(Instant.now());
        log.info("Purged {} expired/revoked refresh tokens.", deleted);
    }
}
