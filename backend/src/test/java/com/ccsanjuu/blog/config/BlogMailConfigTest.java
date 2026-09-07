package com.ccsanjuu.blog.config;

import com.ccsanjuu.blog.properties.BlogMailProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BlogMailConfigTest {

    @Test
    void disabledMailShouldAllowEmptySmtpConfiguration() {
        BlogMailProperties properties = new BlogMailProperties();
        properties.setEnabled(false);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        assertDoesNotThrow(() -> new BlogMailConfig(properties, environment));
    }

    @Test
    void enabledMailShouldRejectIncompleteConfiguration() {
        BlogMailProperties properties = new BlogMailProperties();
        properties.setEnabled(true);
        properties.setFrom("noreply@example.com");
        properties.setFrontendBaseUrl("https://blog.example.com");
        MockEnvironment environment = new MockEnvironment()
                .withProperty("spring.mail.host", "smtp.example.com")
                .withProperty("spring.mail.username", "noreply@example.com");

        assertThrows(IllegalStateException.class, () -> new BlogMailConfig(properties, environment));
    }

    @Test
    void enabledMailShouldRejectMissingFrontendUrl() {
        BlogMailProperties properties = completeProperties(null);

        assertThrows(IllegalStateException.class, () -> new BlogMailConfig(properties, completeEnvironment()));
    }

    @Test
    void enabledMailShouldAllowLocalHttpOutsideProduction() {
        BlogMailProperties properties = completeProperties("http://localhost:5173");

        assertDoesNotThrow(() -> new BlogMailConfig(properties, completeEnvironment()));
    }

    @Test
    void enabledMailShouldRejectNonHttpsFrontendUrlInProduction() {
        BlogMailProperties properties = completeProperties("http://blog.example.com");
        MockEnvironment environment = completeEnvironment();
        environment.setActiveProfiles("prod");

        assertThrows(IllegalStateException.class, () -> new BlogMailConfig(properties, environment));
    }

    @Test
    void enabledMailShouldAllowHttpsFrontendUrlInProduction() {
        BlogMailProperties properties = completeProperties("https://blog.example.com");
        MockEnvironment environment = completeEnvironment();
        environment.setActiveProfiles("prod");

        assertDoesNotThrow(() -> new BlogMailConfig(properties, environment));
    }

    private BlogMailProperties completeProperties(String frontendBaseUrl) {
        BlogMailProperties properties = new BlogMailProperties();
        properties.setEnabled(true);
        properties.setFrom("noreply@example.com");
        properties.setFrontendBaseUrl(frontendBaseUrl);
        return properties;
    }

    private MockEnvironment completeEnvironment() {
        return new MockEnvironment()
                .withProperty("spring.mail.host", "smtp.example.com")
                .withProperty("spring.mail.username", "noreply@example.com")
                .withProperty("spring.mail.password", "authorization-code");
    }
}
