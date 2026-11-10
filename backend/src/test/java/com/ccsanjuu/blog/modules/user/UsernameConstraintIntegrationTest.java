package com.ccsanjuu.blog.modules.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class UsernameConstraintIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void databaseShouldAllowTwoCharacterUsernameWithoutFirstCharacterRestriction() {
        assertDoesNotThrow(() -> insertUser("_a", "username-valid@example.com"));
    }

    @Test
    void databaseShouldRejectChineseUsername() {
        assertThrows(
                DataIntegrityViolationException.class,
                () -> insertUser("中文用户", "username-chinese@example.com")
        );
    }

    @Test
    void databaseShouldRejectUnsupportedUsernameCharacters() {
        assertThrows(
                DataIntegrityViolationException.class,
                () -> insertUser("alice.user", "username-dot@example.com")
        );
    }

    private void insertUser(String username, String email) {
        jdbcTemplate.update(
                "INSERT INTO blog_user (username, nickname, email, password_hash) VALUES (?, ?, ?, ?)",
                username,
                username,
                email,
                "test-password-hash"
        );
    }
}
