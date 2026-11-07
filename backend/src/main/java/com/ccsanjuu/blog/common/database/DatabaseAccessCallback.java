package com.ccsanjuu.blog.common.database;

import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.postgresql.util.PasswordUtil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

/**
 * 使用 Flyway 管理连接准备应用账号，并在迁移完成后授予业务访问权限。
 * 环境账号不写入版本 migration，以便恢复不包含角色和 ACL 的逻辑备份。
 */
public final class DatabaseAccessCallback implements Callback {

    private final String username;
    private final String password;

    public DatabaseAccessCallback(String username, String password) {
        if (username == null || !username.matches("[a-z][a-z0-9_]{0,62}")) {
            throw new IllegalArgumentException("应用数据库用户名必须为小写字母开头的字母、数字或下划线，最长 63 个字符");
        }
        if (password == null || password.isBlank() || password.indexOf('\0') >= 0) {
            throw new IllegalArgumentException("应用数据库密码不能为空或包含空字符");
        }
        this.username = username;
        this.password = password;
    }

    @Override
    public boolean supports(Event event, Context context) {
        return event == Event.BEFORE_MIGRATE || event == Event.AFTER_MIGRATE;
    }

    @Override
    public boolean canHandleInTransaction(Event event, Context context) {
        return true;
    }

    @Override
    public void handle(Event event, Context context) {
        try {
            if (event == Event.BEFORE_MIGRATE) {
                prepareAccounts(context.getConnection());
            }
            else if (event == Event.AFTER_MIGRATE) {
                grantPermissions(context.getConnection());
                validateCredentials(context.getConnection().getMetaData().getURL());
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("数据库账号准备或授权失败，SQLState=" + exception.getSQLState());
        }
    }

    @Override
    public String getCallbackName() {
        return "database-application-access";
    }

    /**
     * 创建缺失的受限账号并授予数据库连接权限，不创建或迁移业务对象。
     *
     * @param connection 管理连接；调用方负责事务
     * @throws SQLException 账号查询或创建失败
     */
    public void prepareAccounts(Connection connection) throws SQLException {
        String database;
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("select current_user, current_database()")) {
            result.next();
            if (username.equals(result.getString(1))) {
                throw new IllegalArgumentException("数据库管理账号与应用账号必须不同");
            }
            database = result.getString(2);
        }

        // 同一库中的并发初始化先串行检查角色，避免同时 CREATE ROLE。
        try (PreparedStatement statement = connection.prepareStatement(
                "select pg_advisory_xact_lock(1630271181, hashtext(?))")) {
            statement.setString(1, username);
            statement.execute();
        }
        boolean exists;
        try (PreparedStatement statement = connection.prepareStatement("""
                select r.rolsuper, r.rolcreatedb, r.rolcreaterole, r.rolreplication, r.rolbypassrls, r.rolcanlogin,
                       exists(select 1 from pg_auth_members m where m.member = r.oid) as has_membership,
                       exists(select 1 from pg_database d where d.datdba = r.oid)
                           or exists(select 1 from pg_namespace n where n.nspowner = r.oid)
                           or exists(select 1 from pg_class c where c.relowner = r.oid)
                           or exists(select 1 from pg_proc p where p.proowner = r.oid) as owns_objects
                from pg_roles r where r.rolname = ?
                """)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                exists = result.next();
                if (exists && (result.getBoolean(1) || result.getBoolean(2) || result.getBoolean(3)
                        || result.getBoolean(4) || result.getBoolean(5) || !result.getBoolean(6)
                        || result.getBoolean(7) || result.getBoolean(8))) {
                    throw new IllegalArgumentException("已有应用数据库账号具有管理权限、对象所有权、角色成员身份或不能登录，请使用独立受限账号");
                }
            }
        }
        if (!exists) {
            // 使用 PostgreSQL 驱动生成 SCRAM 校验值，SQL 不包含明文密码，异常也不输出语句正文。
            char[] characters = password.toCharArray();
            try {
                String verifier = PasswordUtil.encodeScramSha256(characters);
                execute(connection, "create role " + identifier(username)
                        + " login noinherit nosuperuser nocreatedb nocreaterole noreplication nobypassrls password '"
                        + verifier + "'");
            } finally {
                Arrays.fill(characters, '\0');
            }
        }
        execute(connection, "revoke all privileges on database " + identifier(database) + " from public");
        execute(connection, "revoke create, temporary on database " + identifier(database) + " from " + identifier(username));
        execute(connection, "grant connect on database " + identifier(database) + " to " + identifier(username));
        execute(connection, "revoke create on schema public from public");
        execute(connection, "revoke create on schema public from " + identifier(username));
    }

    /**
     * 建立真实应用连接验证密码，不静默覆盖已有账号凭据。
     *
     * @param url 目标数据库连接地址
     * @throws SQLException 应用认证失败
     */
    public void validateCredentials(String url) throws SQLException {
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            if (!connection.isValid(5)) {
                throw new SQLException("应用数据库连接不可用", "08001");
            }
        }
    }

    /**
     * 转义 PostgreSQL 标识符，数据库和对象名称可能包含引号。
     *
     * @param value 标识符原文
     * @return 双引号包围的安全标识符
     */
    private String identifier(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    /**
     * 执行维护 SQL，不记录包含凭据的 SQL 文本。
     *
     * @param connection 管理连接
     * @param sql 已转义的维护语句
     * @throws SQLException 执行失败
     */
    private void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    /**
     * 补齐现有业务对象及后续新对象的权限，Flyway 历史表始终不向应用账号开放。
     *
     * @param connection 实际执行迁移的管理连接
     * @throws SQLException 授权失败
     */
    private void grantPermissions(Connection connection) throws SQLException {
        String role = identifier(username);
        execute(connection, "revoke create on schema public from public");
        execute(connection, "revoke create on schema public from " + role);
        execute(connection, "grant usage on schema public to " + role);
        execute(connection, "alter default privileges in schema public grant select, insert, update, delete on tables to " + role);
        execute(connection, "alter default privileges in schema public grant usage, select on sequences to " + role);
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery("""
                     select relname, relkind from pg_class
                     where relnamespace = 'public'::regnamespace
                       and relkind in ('r', 'p', 'S') and starts_with(relname, 'blog_')
                     """)) {
            while (result.next()) {
                String object = "public." + identifier(result.getString(1));
                if ("S".equals(result.getString(2))) {
                    execute(connection, "revoke all privileges on sequence " + object + " from " + role);
                    execute(connection, "grant usage, select on sequence " + object + " to " + role);
                }
                else {
                    execute(connection, "revoke all privileges on table " + object + " from " + role);
                    execute(connection, "grant select, insert, update, delete on table " + object + " to " + role);
                }
            }
        }
        execute(connection, "revoke all privileges on table public.flyway_schema_history from " + role);
    }
}
