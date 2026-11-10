package com.ccsanjuu.blog.common.util;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHashGenerationTest {

    private static final String RAW_PASSWORD = "123456";

    @Test
    @Disabled("Manual helper: run locally when you need to generate a BCrypt hash for seeded accounts.")
    void shouldGenerateBcryptHashForSeedPassword() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encodedPassword = passwordEncoder.encode(RAW_PASSWORD);

        System.out.println("BCrypt hash for seed password: " + encodedPassword);
        assertTrue(passwordEncoder.matches(RAW_PASSWORD, encodedPassword));
    }
}
