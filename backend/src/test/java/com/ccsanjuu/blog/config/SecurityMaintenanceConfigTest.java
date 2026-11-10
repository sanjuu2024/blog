package com.ccsanjuu.blog.config;

import com.ccsanjuu.blog.modules.auth.mapper.AuthMapper;
import com.ccsanjuu.blog.modules.security.mapper.SecurityEventMapper;
import com.ccsanjuu.blog.modules.security.service.SecurityMaintenanceTask;
import com.ccsanjuu.blog.properties.SecurityMaintenanceProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;
import org.springframework.scheduling.config.CronTask;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class SecurityMaintenanceConfigTest {

    @Test
    void applicationYamlShouldBindRetentionAndRegisterTheConfiguredCron() throws IOException {
        contextRunner().run(context -> {
            assertNull(context.getStartupFailure());
            var properties = context.getBean(SecurityMaintenanceProperties.class);
            assertEquals(90, properties.getEventRetentionDays());
            assertEquals(7, properties.getSessionRetentionDays());
            assertEquals(Boolean.TRUE, context.getEnvironment().getProperty("blog.security-maintenance.enabled", Boolean.class));
            var tasks = context.getBean(ScheduledAnnotationBeanPostProcessor.class).getScheduledTasks();
            assertEquals(1, tasks.size());
            assertEquals("0 30 3 * * *", ((CronTask) tasks.iterator().next().getTask()).getExpression());
        });
    }

    @Test
    void overridesShouldChangeRetentionAndCron() throws IOException {
        contextRunner().withPropertyValues(
                "blog.security-maintenance.event-retention-days=120",
                "blog.security-maintenance.session-retention-days=14",
                "blog.security-maintenance.cron=0 0 4 * * *"
        ).run(context -> {
            assertNull(context.getStartupFailure());
            var properties = context.getBean(SecurityMaintenanceProperties.class);
            assertEquals(120, properties.getEventRetentionDays());
            assertEquals(14, properties.getSessionRetentionDays());
            var tasks = context.getBean(ScheduledAnnotationBeanPostProcessor.class).getScheduledTasks();
            assertEquals("0 0 4 * * *", ((CronTask) tasks.iterator().next().getTask()).getExpression());
        });
    }

    @Test
    void disabledMaintenanceShouldNotRegisterScheduledTasks() throws IOException {
        contextRunner().withPropertyValues("blog.security-maintenance.enabled=false").run(context -> {
            assertNull(context.getStartupFailure());
            assertTrue(context.getBeansOfType(ScheduledAnnotationBeanPostProcessor.class).isEmpty());
            assertFalse(context.containsBean("securityMaintenanceConfig"));
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"test", "bootstrap-admin"})
    void excludedProfilesShouldNotRegisterScheduledTasks(String profile) throws IOException {
        contextRunner().withInitializer(context -> context.getEnvironment().setActiveProfiles(profile)).run(context -> {
            assertNull(context.getStartupFailure());
            assertTrue(context.getBeansOfType(ScheduledAnnotationBeanPostProcessor.class).isEmpty());
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"event-retention-days", "session-retention-days"})
    void nonPositiveRetentionShouldFailConfigurationValidation(String property) throws IOException {
        contextRunner().withPropertyValues("blog.security-maintenance." + property + "=0").run(context ->
                assertTrue(context.getStartupFailure() != null));
    }

    /**
     * 读取实际通用 YAML，仅启动维护配置，避免依赖数据库和真实环境变量。
     *
     * @return 用于验证配置绑定和调度注册的上下文
     * @throws IOException 配置文件读取失败
     */
    private ApplicationContextRunner contextRunner() throws IOException {
        var source = new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yaml")).getFirst();
        return new ApplicationContextRunner()
                .withInitializer(context -> context.getEnvironment().getPropertySources().addLast(source))
                .withUserConfiguration(MaintenanceTestConfiguration.class);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(SecurityMaintenanceProperties.class)
    @Import(SecurityMaintenanceConfig.class)
    static class MaintenanceTestConfiguration {

        @Bean
        SecurityMaintenanceTask securityMaintenanceTask(SecurityMaintenanceProperties properties) {
            return new SecurityMaintenanceTask(mock(SecurityEventMapper.class), mock(AuthMapper.class), properties);
        }
    }
}
