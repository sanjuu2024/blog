package com.ccsanjuu.blog.modules.user;

import com.ccsanjuu.blog.modules.user.model.dto.AdminBootstrapRequestDTO;
import com.ccsanjuu.blog.modules.user.service.ProductionAdminBootstrapService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ProductionAdminBootstrapIntegrationTest {

    @Autowired
    private ProductionAdminBootstrapService bootstrapService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void shouldPersistOnlyOneProductionAdministratorWithEncodedPassword() {
        jdbcTemplate.update("delete from blog_user where role = 'ADMIN'");
        AdminBootstrapRequestDTO request = new AdminBootstrapRequestDTO(
                "prod_admin",
                "生产管理员",
                "prod-admin@example.com",
                "StrongPass1!"
        );

        OffsetDateTime beforeBootstrap = OffsetDateTime.now(ZoneOffset.UTC);
        Long userId = bootstrapService.bootstrap(request);
        OffsetDateTime afterBootstrap = OffsetDateTime.now(ZoneOffset.UTC);

        Map<String, Object> admin = jdbcTemplate.queryForMap(
                "select username, nickname, email, password_hash, role, status, token_version, email_verified "
                        + "from blog_user where id = ?",
                userId
        );
        assertEquals("prod_admin", admin.get("username"));
        assertEquals("生产管理员", admin.get("nickname"));
        assertEquals("prod-admin@example.com", admin.get("email"));
        assertEquals("ADMIN", admin.get("role"));
        assertEquals("ACTIVE", admin.get("status"));
        assertEquals(0L, admin.get("token_version"));
        assertEquals(true, admin.get("email_verified"));
        OffsetDateTime emailVerifiedAt = jdbcTemplate.queryForObject(
                "select email_verified_at from blog_user where id = ?", OffsetDateTime.class, userId
        );
        assertNotNull(emailVerifiedAt);
        // PostgreSQL 按微秒存储，允许写入时的精度舍入。
        assertFalse(emailVerifiedAt.isBefore(beforeBootstrap.minusNanos(1_000)));
        assertFalse(emailVerifiedAt.isAfter(afterBootstrap.plusNanos(1_000)));
        assertFalse("StrongPass1!".equals(admin.get("password_hash")));
        assertTrue(passwordEncoder.matches("StrongPass1!", admin.get("password_hash").toString()));
        assertThrows(IllegalStateException.class, () -> bootstrapService.bootstrap(request));
        assertEquals(emailVerifiedAt, jdbcTemplate.queryForObject(
                "select email_verified_at from blog_user where id = ?", OffsetDateTime.class, userId
        ));
    }
}
