package com.ccsanjuu.blog.properties;

import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlogPropertiesTest {

    @Test
    void siteProfileFieldsMustBeNonBlank() {
        BlogProperties properties = new BlogProperties();
        properties.setSiteUrl(URI.create("https://blog.example.com"));

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(properties);

            assertTrue(violations.stream().anyMatch(item -> item.getPropertyPath().toString().equals("appName")));
            assertTrue(violations.stream().anyMatch(item -> item.getPropertyPath().toString().equals("description")));
            assertTrue(violations.stream().anyMatch(item -> item.getPropertyPath().toString().equals("logoUrl")));
        }
    }

    @Test
    void localProfileAllowsHttpSiteOrigin() {
        BlogProperties properties = validProperties("http://localhost:5173");

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(properties).isEmpty());
        }
    }

    @Test
    void siteOriginMustBeConfigured() {
        BlogProperties properties = validProperties("https://blog.example.com");
        properties.setSiteUrl(null);

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(properties);

            assertTrue(violations.stream().anyMatch(item -> item.getPropertyPath().toString().equals("siteUrlValid")));
        }
    }

    @Test
    void productionProfileRejectsHttpSiteOrigin() {
        BlogProperties properties = validProperties("http://blog.example.com");
        properties.setRequireHttpsSiteUrl(true);

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(properties);

            assertTrue(violations.stream().anyMatch(item -> item.getPropertyPath().toString().equals("siteUrlValid")));
        }
    }

    @Test
    void localProfileAllowsHttpLogoUrl() {
        BlogProperties properties = validProperties("https://blog.example.com");
        properties.setLogoUrl("http://img.example.com/site-logo.png");

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(properties).isEmpty());
        }
    }

    @Test
    void productionProfileRequiresHttpsLogoUrl() {
        BlogProperties properties = validProperties("https://blog.example.com");
        properties.setLogoUrl("http://img.example.com/site-logo.png");
        properties.setRequireHttpsLogoUrl(true);

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var violations = factory.getValidator().validate(properties);

            assertFalse(violations.isEmpty());
            assertTrue(violations.stream().anyMatch(item -> item.getPropertyPath().toString().equals("logoUrlValid")));
        }
    }

    private BlogProperties validProperties(String siteUrl) {
        BlogProperties properties = new BlogProperties();
        properties.setAppName("Example Blog");
        properties.setSiteUrl(URI.create(siteUrl));
        properties.setDescription("A blog for testing");
        properties.setLogoUrl("https://img.example.com/site-logo.png");
        return properties;
    }
}
