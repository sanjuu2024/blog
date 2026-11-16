package com.ccsanjuu.blog.modules.friendlink;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.ActiveProfiles;

import java.sql.SQLException;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class FriendLinkSectionMigrationIntegrationTest {

    @Autowired
    private Flyway flyway;

    @Test
    void migrationShouldPreservePeerRecordsAndRejectTheRemovedSection() throws Exception {
        // 临时表遮蔽同名业务表，使用迁移管理连接验证历史数据，整个事务结束后回滚。
        try (var connection = flyway.getConfiguration().getDataSource().getConnection()) {
            connection.setAutoCommit(false);
            try (var statement = connection.createStatement()) {
                statement.execute("CREATE TEMP TABLE blog_friend_link (LIKE public.blog_friend_link INCLUDING DEFAULTS)");
                statement.execute("ALTER TABLE blog_friend_link ADD CONSTRAINT blog_friend_link_section_check "
                        + "CHECK (section IN ('FRIENDS', 'EXPERTS', 'PEERS'))");
                statement.executeUpdate("""
                        INSERT INTO blog_friend_link
                            (id, name, url, logo_url, description, section, sort_no, status, created_at, updated_at)
                        VALUES
                            (1, 'friend', 'https://friend.example.com', '', '', 'FRIENDS', 100, 'ENABLED',
                             '2026-11-01T00:00:00Z', '2026-11-02T00:00:00Z'),
                            (2, 'expert', 'https://expert.example.com', '', '', 'EXPERTS', 200, 'ENABLED',
                             '2026-11-01T00:00:00Z', '2026-11-02T00:00:00Z'),
                            (3, 'peer', 'https://peer.example.com', 'https://peer.example.com/logo.png', 'description',
                             'PEERS', 250, 'DISABLED', '2026-11-01T00:00:00Z', '2026-11-02T00:00:00Z')
                        """);

                ScriptUtils.executeSqlScript(connection,
                        new ClassPathResource("db/migration/V1.3.0__remove_peer_friend_link_section.sql"));

                try (var rows = statement.executeQuery("SELECT * FROM blog_friend_link ORDER BY id")) {
                    assertTrue(rows.next());
                    assertEquals("FRIENDS", rows.getString("section"));
                    assertTrue(rows.next());
                    assertEquals("EXPERTS", rows.getString("section"));
                    assertTrue(rows.next());
                    assertEquals(3L, rows.getLong("id"));
                    assertEquals("FRIENDS", rows.getString("section"));
                    assertEquals("peer", rows.getString("name"));
                    assertEquals("https://peer.example.com", rows.getString("url"));
                    assertEquals("https://peer.example.com/logo.png", rows.getString("logo_url"));
                    assertEquals("description", rows.getString("description"));
                    assertEquals(250, rows.getInt("sort_no"));
                    assertEquals("DISABLED", rows.getString("status"));
                    assertEquals(Instant.parse("2026-11-01T00:00:00Z"), rows.getTimestamp("created_at").toInstant());
                    assertEquals(Instant.parse("2026-11-02T00:00:00Z"), rows.getTimestamp("updated_at").toInstant());
                    assertFalse(rows.next());
                }
                var exception = assertThrows(SQLException.class,
                        () -> statement.executeUpdate("UPDATE blog_friend_link SET section = 'PEERS' WHERE id = 3"));
                assertEquals("23514", exception.getSQLState());
            } finally {
                connection.rollback();
            }
        }
    }
}
