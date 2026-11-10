package com.ccsanjuu.blog.modules.security;

import com.ccsanjuu.blog.modules.auth.mapper.AuthMapper;
import com.ccsanjuu.blog.modules.security.mapper.SecurityEventMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SecurityMaintenanceIntegrationTest {

    @Autowired
    private SecurityEventMapper securityEventMapper;

    @Autowired
    private AuthMapper authMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void shouldDeleteOnlyEventsOlderThanRetention() {
        OffsetDateTime cutoff = OffsetDateTime.now(ZoneOffset.UTC).minusDays(90);
        String account = "retention-" + UUID.randomUUID();
        for (var timestamp : new OffsetDateTime[]{cutoff.minusSeconds(1), cutoff, cutoff.plusSeconds(1)}) {
            jdbc.update("INSERT INTO blog_security_event (event_type, outcome, account, description, created_at) "
                    + "VALUES ('LOGIN', 'FAILURE', ?, '测试事件', ?)", account, timestamp);
        }

        securityEventMapper.deleteExpiredBatch(cutoff, 1000);

        assertEquals(2L, jdbc.queryForObject("SELECT COUNT(*) FROM blog_security_event WHERE account = ?", Long.class, account));
    }

    @Test
    void shouldKeepUnexpiredRevokedSessionsAndRecentlyExpiredSessions() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        String prefix = UUID.randomUUID().toString();
        jdbc.update("INSERT INTO blog_auth_session (user_id, token_jti, token_hash, status, expires_at) "
                + "VALUES (10001, ?, 'test-hash', 'ACTIVE', ?)", prefix + "old", now.minusDays(8));
        jdbc.update("INSERT INTO blog_auth_session (user_id, token_jti, token_hash, status, expires_at) "
                + "VALUES (10001, ?, 'test-hash', 'ACTIVE', ?)", prefix + "recent", now.minusDays(1));
        jdbc.update("INSERT INTO blog_auth_session (user_id, token_jti, token_hash, status, expires_at) "
                + "VALUES (10001, ?, 'test-hash', 'REVOKED', ?)", prefix + "revoked", now.plusDays(1));

        authMapper.deleteExpiredBatch(now.minusDays(7), 1000);

        assertEquals(0L, jdbc.queryForObject("SELECT COUNT(*) FROM blog_auth_session WHERE token_jti = ?", Long.class, prefix + "old"));
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM blog_auth_session WHERE token_jti = ?", Long.class, prefix + "recent"));
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM blog_auth_session WHERE token_jti = ?", Long.class, prefix + "revoked"));
    }
}
