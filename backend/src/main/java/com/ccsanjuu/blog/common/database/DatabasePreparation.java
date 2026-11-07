package com.ccsanjuu.blog.common.database;

import org.flywaydb.core.Flyway;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

/**
 * 复用后端镜像的一次性数据库维护入口，不启动 Web、业务组件或第三方服务。
 */
public final class DatabasePreparation {

    private DatabasePreparation() {
    }

    /**
     * 根据环境配置准备账号，或执行迁移和授权。
     *
     * @param environment 已由 Compose 或 IDE 显式注入的环境变量
     * @param accountsOnly 为 true 时只准备账号，供恢复备份前使用
     */
    public static void run(Map<String, String> environment, boolean accountsOnly) {
        String url = required(environment, "BLOG_DB_URL");
        String adminUsername = required(environment, "POSTGRES_ADMIN_USERNAME");
        String adminPassword = required(environment, "POSTGRES_ADMIN_PASSWORD");
        DatabaseAccessCallback callback = new DatabaseAccessCallback(
                required(environment, "POSTGRES_APP_USERNAME"), required(environment, "POSTGRES_APP_PASSWORD"));

        if (accountsOnly) {
            try (Connection connection = DriverManager.getConnection(url, adminUsername, adminPassword)) {
                connection.setAutoCommit(false);
                callback.prepareAccounts(connection);
                connection.commit();
                callback.validateCredentials(url);
            } catch (SQLException exception) {
                throw new IllegalStateException("数据库账号准备失败，SQLState=" + exception.getSQLState());
            }
            System.out.println("DATABASE_ACCOUNTS_READY");
        }
        else {
            Flyway.configure()
                    .dataSource(url, adminUsername, adminPassword)
                    .locations("classpath:db/migration")
                    .baselineOnMigrate(true)
                    .validateOnMigrate(true)
                    .callbacks(callback)
                    .load()
                    .migrate();
            System.out.println("DATABASE_PREPARATION_SUCCESS");
        }
    }

    /**
     * 检查维护任务必需配置，不输出配置值。
     *
     * @param environment 环境变量
     * @param name 配置名称
     * @return 非空配置值
     */
    private static String required(Map<String, String> environment, String name) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " 不能为空");
        }
        return value;
    }
}
