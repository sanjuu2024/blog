package com.ccsanjuu.blog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest
class BackendApplicationTests {

    @Autowired
    private Environment environment;

    @Test
    void contextLoads() {
    }

    @Test
    void smtpTimeoutsShouldBePositive() {
        List.of(
                        "spring.mail.properties.mail.smtp.connectiontimeout",
                        "spring.mail.properties.mail.smtp.timeout",
                        "spring.mail.properties.mail.smtp.writetimeout"
                )
                .forEach(name -> assertTrue(Long.parseLong(environment.getRequiredProperty(name)) > 0));
    }

}
