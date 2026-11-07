package com.ccsanjuu.blog.config;

import com.ccsanjuu.blog.common.database.DatabaseAccessCallback;
import org.flywaydb.core.api.callback.Callback;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 开发和测试保留启动时自动迁移，通过独立的 Flyway 管理连接准备应用权限。
 */
@Configuration
@Profile({"dev", "test"})
public class DatabaseAccessConfig {

    /**
     * 为独立的 Flyway 管理连接注册应用账号准备和授权回调。
     *
     * @param properties 业务数据源配置，其中用户名和密码为受限账号
     * @return Flyway 迁移回调
     */
    @Bean
    public Callback databaseAccessCallback(DataSourceProperties properties) {
        return new DatabaseAccessCallback(properties.getUsername(), properties.getPassword());
    }
}
