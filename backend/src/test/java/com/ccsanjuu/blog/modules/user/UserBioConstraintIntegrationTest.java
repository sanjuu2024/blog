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
class UserBioConstraintIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void databaseShouldAllowOneHundredCharacterBio() {
        assertDoesNotThrow(() -> insertUser("bio_valid", "bio-valid@example.com", "a".repeat(100)));
    }

    @Test
    void databaseShouldRejectBioLongerThanOneHundredCharacters() {
        assertThrows(
                DataIntegrityViolationException.class,
                () -> insertUser("bio_invalid", "bio-invalid@example.com", "a".repeat(101))
        );
    }

    private void insertUser(String username, String email, String bio) {
        jdbcTemplate.update(
                "INSERT INTO blog_user (username, nickname, email, password_hash, bio) VALUES (?, ?, ?, ?, ?)",
                username,
                username,
                email,
                "test-password-hash",
                bio
        );
    }
}
