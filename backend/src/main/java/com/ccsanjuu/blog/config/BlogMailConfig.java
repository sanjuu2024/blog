package com.ccsanjuu.blog.config;

import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.properties.BlogMailProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.net.URI;

@Configuration
@EnableConfigurationProperties(BlogMailProperties.class)
public class BlogMailConfig {

    public BlogMailConfig(BlogMailProperties properties, Environment environment) {
        if (!properties.isEnabled()) {
            return;
        }
        if (properties.getFrom() == null || properties.getFrom().isBlank()
                || properties.getFrontendBaseUrl() == null || properties.getFrontendBaseUrl().isBlank()
                || environment.getProperty("spring.mail.host", "").isBlank()
                || environment.getProperty("spring.mail.username", "").isBlank()
                || environment.getProperty("spring.mail.password", "").isBlank()
                || (environment.acceptsProfiles(Profiles.of("prod"))
                && !isHttpsUrl(properties.getFrontendBaseUrl()))) {
            throw new IllegalStateException(ResultCode.MESSAGE_MAIL_CONFIG_INVALID.getMessage());
        }
    }

    /**
     * 留言通知使用独立线程池，避免 SMTP 请求占用 Web 请求线程。
     *
     * @return 留言邮件任务执行器
     */
    @Bean
    public TaskExecutor messageMailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("message-mail-");
        executor.initialize();
        return executor;
    }

    /**
     * 校验生产环境邮件中的前端链接是否为完整 HTTPS 地址。
     *
     * @param value 前端站点根地址
     * @return 是否为包含主机名的 HTTPS 地址
     */
    private boolean isHttpsUrl(String value) {
        try {
            URI uri = URI.create(value);
            return "https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
