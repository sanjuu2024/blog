package com.ccsanjuu.blog.config;

import org.flywaydb.core.api.callback.Callback;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DatabaseAccessConfigTest {

    @ParameterizedTest
    @ValueSource(strings = {"dev", "test", "prod"})
    void shouldOnlyRegisterAccountMaintenanceForDevelopmentAndTests(String profile) {
        DataSourceProperties properties = new DataSourceProperties();
        properties.setUsername("blog_app");
        properties.setPassword("test-password");
        new ApplicationContextRunner()
                .withPropertyValues("spring.profiles.active=" + profile)
                .withBean(DataSourceProperties.class, () -> properties)
                .withUserConfiguration(DatabaseAccessConfig.class)
                .run(context -> assertEquals(profile.equals("prod") ? 0 : 1, context.getBeansOfType(Callback.class).size()));
    }
}
