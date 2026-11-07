package com.ccsanjuu.blog.common.database;

import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatabaseAccessCallbackTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"postgres; drop role test", "app-user", "Admin", "1user"})
    void shouldRejectUnsafeRoleNames(String username) {
        assertThrows(IllegalArgumentException.class, () -> new DatabaseAccessCallback(username, "test-password"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "test\0password"})
    void shouldRequireAnApplicationPassword(String password) {
        assertThrows(IllegalArgumentException.class, () -> new DatabaseAccessCallback("blog_app", password));
    }

    @Test
    void shouldOnlyHandleBeforeAndAfterMigration() {
        var callback = new DatabaseAccessCallback("blog_app", "test-password");
        assertTrue(callback.supports(Event.BEFORE_MIGRATE, null));
        assertTrue(callback.supports(Event.AFTER_MIGRATE, null));
        assertFalse(callback.supports(Event.AFTER_VALIDATE, null));
        assertTrue(callback.canHandleInTransaction(Event.AFTER_MIGRATE, null));
        assertEquals("database-application-access", callback.getCallbackName());
    }

    @Test
    void shouldHideSqlExceptionDetails() throws SQLException {
        Context context = mock(Context.class);
        Connection connection = mock(Connection.class);
        when(context.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenThrow(new SQLException("test-password sql payload", "08001"));

        var exception = assertThrows(IllegalStateException.class,
                () -> new DatabaseAccessCallback("blog_app", "test-password").handle(Event.BEFORE_MIGRATE, context));

        assertTrue(exception.getMessage().contains("08001"));
        assertFalse(exception.getMessage().contains("test-password"));
        assertNull(exception.getCause());
    }

    @Test
    void preparationShouldRejectMissingConfigurationBeforeConnecting() {
        var exception = assertThrows(IllegalArgumentException.class,
                () -> DatabasePreparation.run(Map.of(), true));
        assertEquals("BLOG_DB_URL 不能为空", exception.getMessage());
    }
}
