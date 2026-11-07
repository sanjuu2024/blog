package com.ccsanjuu.blog.common.database;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 使用测试管理连接创建独立临时库，验证真实 PostgreSQL 的角色、Flyway、默认授权和恢复路径。
 */
class DatabaseAccessIntegrationTest {

    @TempDir
    Path migrations;

    private String adminUrl;
    private String adminUsername;
    private String adminPassword;
    private String database;
    private String username;
    private String url;
    private final String password = "test'password\\with-special-characters";

    @BeforeEach
    void createIsolatedDatabase() throws SQLException {
        adminUrl = System.getenv("BLOG_TEST_DB_URL");
        adminUsername = System.getenv("POSTGRES_TEST_ADMIN_USERNAME");
        adminPassword = System.getenv("POSTGRES_TEST_ADMIN_PASSWORD");
        String suffix = UUID.randomUUID().toString().replace("-", "");
        database = "accounts_test_" + suffix;
        username = "app_" + suffix;
        try (Connection connection = adminConnection(); var statement = connection.createStatement()) {
            statement.execute("create database " + database);
        }
        url = adminUrl.replaceFirst("/[^/?]+(?=\\?|$)", "/" + database);
    }

    @AfterEach
    void removeIsolatedDatabaseAndRole() throws SQLException {
        if (adminUrl != null && database != null) {
            try (Connection connection = adminConnection(); var statement = connection.createStatement()) {
                statement.execute("drop database if exists " + database + " with (force)");
                statement.execute("drop role if exists " + username);
            }
        }
    }

    @Test
    void accountsOnlyShouldLeaveBusinessDatabaseEmptyAndKeepExistingPassword() throws SQLException {
        DatabasePreparation.run(environment(), true);
        DatabasePreparation.run(environment(), true);
        try (Connection connection = applicationConnection(); var statement = connection.createStatement();
             ResultSet result = statement.executeQuery("select count(*) from pg_class where relnamespace = 'public'::regnamespace")) {
            result.next();
            assertEquals(0, result.getInt(1));
        }
        try (Connection connection = DriverManager.getConnection(url, adminUsername, adminPassword);
             var statement = connection.createStatement(); ResultSet result = statement.executeQuery(
                     "select rolsuper, rolcreatedb, rolcreaterole, rolreplication, rolbypassrls from pg_roles where rolname = '" + username + "'")) {
            result.next();
            for (int i = 1; i <= 5; i++) {
                assertFalse(result.getBoolean(i));
            }
        }
        assertThrows(SQLException.class, () -> DriverManager.getConnection(url, username, "wrong-password"));
    }

    @Test
    void migrationShouldGrantBusinessWritesButDenyDdlAndHistoryAccess() throws SQLException {
        migrate();
        try (Connection connection = applicationConnection(); var statement = connection.createStatement()) {
            statement.execute("insert into blog_tag(name, description, status) values ('permission-test', '', 'ENABLED')");
            statement.execute("update blog_tag set description = 'updated' where name = 'permission-test'");
            try (ResultSet result = statement.executeQuery("select description from blog_tag where name = 'permission-test'")) {
                result.next();
                assertEquals("updated", result.getString(1));
            }
            statement.execute("delete from blog_tag where name = 'permission-test'");
            for (String sql : new String[]{"create table public.forbidden(id integer)", "drop table blog_tag",
                    "truncate blog_tag", "select * from flyway_schema_history", "delete from flyway_schema_history",
                    "create role forbidden_role", "create schema forbidden_schema"}) {
                assertEquals("42501", assertThrows(SQLException.class, () -> statement.execute(sql)).getSQLState());
            }
        }
    }

    @Test
    void newMigrationShouldAutomaticallyGrantTableAndSequenceAccess() throws Exception {
        migrate();
        Files.writeString(migrations.resolve("V99.0.0__permission_probe.sql"),
                "create table blog_permission_probe(id bigserial primary key, content text)");
        flyway("classpath:db/migration", "filesystem:" + migrations.toAbsolutePath()).migrate();
        try (Connection connection = applicationConnection(); var statement = connection.createStatement()) {
            statement.execute("insert into blog_permission_probe(content) values ('new-table')");
            try (ResultSet result = statement.executeQuery("select content from blog_permission_probe")) {
                result.next();
                assertEquals("new-table", result.getString(1));
            }
        }
        // 默认授权覆盖下一次管理连接创建的业务对象，无需逐个 migration 编写 GRANT。
        try (Connection connection = DriverManager.getConnection(url, adminUsername, adminPassword);
             var statement = connection.createStatement()) {
            statement.execute("create table blog_default_probe(id bigserial primary key)");
        }
        try (Connection connection = applicationConnection(); var statement = connection.createStatement()) {
            statement.execute("insert into blog_default_probe default values");
        }
    }

    @Test
    void existingDatabaseAndRestoredPrivilegesShouldBePreparedWithoutLosingData() throws SQLException {
        // 模拟已有 dev 库或 --no-owner/--no-privileges 恢复：先由管理连接建立库结构，应用账号不存在。
        Flyway.configure().dataSource(url, adminUsername, adminPassword).locations("classpath:db/migration").load().migrate();
        try (Connection connection = DriverManager.getConnection(url, adminUsername, adminPassword);
             var statement = connection.createStatement()) {
            statement.execute("insert into blog_tag(name, description, status) values ('preserved-data', '', 'ENABLED')");
        }
        migrate();
        migrate();
        try (Connection connection = applicationConnection(); var statement = connection.createStatement();
             ResultSet result = statement.executeQuery("select count(*) from blog_tag where name = 'preserved-data'")) {
            result.next();
            assertEquals(1, result.getInt(1));
        }
    }

    @Test
    void shouldRejectManagementRolesAndWrongExistingPassword() throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, adminUsername, adminPassword)) {
            connection.setAutoCommit(false);
            assertThrows(IllegalArgumentException.class,
                    () -> new DatabaseAccessCallback(adminUsername, adminPassword).prepareAccounts(connection));
        }
        DatabasePreparation.run(environment(), true);
        assertThrows(RuntimeException.class, () -> Flyway.configure().dataSource(url, adminUsername, adminPassword)
                .locations("classpath:db/migration").callbacks(new DatabaseAccessCallback(username, "wrong-password"))
                .load().migrate());
        try (Connection ignored = applicationConnection()) {
            assertTrue(ignored.isValid(2));
        }
    }

    /**
     * 连接独立测试库所在的管理数据库。
     *
     * @return 管理连接
     * @throws SQLException 连接失败
     */
    private Connection adminConnection() throws SQLException {
        return DriverManager.getConnection(adminUrl, adminUsername, adminPassword);
    }

    /**
     * 使用受限账号连接当前用例的临时数据库。
     *
     * @return 应用连接
     * @throws SQLException 连接失败
     */
    private Connection applicationConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * 构建一次性准备入口使用的虚构环境配置。
     *
     * @return 临时库及账号配置
     */
    private Map<String, String> environment() {
        return Map.of("BLOG_DB_URL", url, "POSTGRES_ADMIN_USERNAME", adminUsername, "POSTGRES_ADMIN_PASSWORD", adminPassword,
                "POSTGRES_APP_USERNAME", username, "POSTGRES_APP_PASSWORD", password);
    }

    /**
     * 为当前临时库配置真实 Flyway 和授权回调。
     *
     * @param locations 迁移资源目录
     * @return Flyway 实例
     */
    private Flyway flyway(String... locations) {
        return Flyway.configure().dataSource(url, adminUsername, adminPassword).locations(locations)
                .callbacks(new DatabaseAccessCallback(username, password)).load();
    }

    /**
     * 执行当前项目的全部迁移与授权。
     */
    private void migrate() {
        flyway("classpath:db/migration").migrate();
    }
}
