package com.ccsanjuu.blog.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@Profile("!test & !bootstrap-admin")
@ConditionalOnProperty(prefix = "blog.security-maintenance", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SecurityMaintenanceConfig {
}
