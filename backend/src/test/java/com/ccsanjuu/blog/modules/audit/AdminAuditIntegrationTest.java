package com.ccsanjuu.blog.modules.audit;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccsanjuu.blog.common.util.JwtUtil;
import com.ccsanjuu.blog.modules.audit.mapper.AdminAuditLogMapper;
import com.ccsanjuu.blog.modules.audit.model.entity.AdminAuditLog;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditAction;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditResourceType;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditResult;
import com.ccsanjuu.blog.modules.tag.mapper.TagMapper;
import com.ccsanjuu.blog.modules.tag.model.entity.Tag;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import com.ccsanjuu.blog.modules.user.model.enums.UserRole;
import com.ccsanjuu.blog.modules.user.model.enums.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminAuditIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecretKey signingKey;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private AdminAuditLogMapper adminAuditLogMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private User admin;
    private String tagName;
    private Long tagId;

    @BeforeEach
    void setUp() {
        String suffix = Long.toString(System.nanoTime(), 36);
        String username = "audit" + suffix;
        admin = User.builder()
                .username(username)
                .nickname(username)
                .email(username + "@example.com")
                .passwordHash("integration-test-password")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .tokenVersion(0L)
                .avatarUrl("")
                .bio("")
                .emailVerified(false)
                .build();
        userMapper.insert(admin);
        tagName = "审计测试-" + suffix;
    }

    @AfterEach
    void tearDown() {
        if (admin != null && admin.getId() != null) {
            adminAuditLogMapper.delete(new LambdaQueryWrapper<AdminAuditLog>()
                    .eq(AdminAuditLog::getOperatorId, admin.getId()));
            stringRedisTemplate.delete("blog:auth:token-version:user:" + admin.getId());
            userMapper.deleteById(admin.getId());
        }
        if (tagId != null) {
            tagMapper.deleteById(tagId);
        }
    }

    @Test
    void shouldPersistSuccessfulAndFailedOperationsAndQueryThem() throws Exception {
        String token = accessToken();
        String requestBody = """
                {"name":"%s","description":"审计集成测试","status":"ENABLED"}
                """.formatted(tagName);

        mockMvc.perform(post("/api/v1/admin/tags")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());
        tagId = tagMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getName, tagName)).getId();

        mockMvc.perform(post("/api/v1/admin/tags")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .header("Authorization", "Bearer " + token)
                        .queryParam("operatorId", admin.getId().toString())
                        .queryParam("resourceType", "TAG")
                        .queryParam("action", "CREATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records[0].result").value("FAILURE"))
                .andExpect(jsonPath("$.data.records[0].failureCode").isNumber())
                .andExpect(jsonPath("$.data.records[1].result").value("SUCCESS"))
                .andExpect(jsonPath("$.data.records[1].resourceId").value(tagId.toString()));

        List<AdminAuditLog> records = adminAuditLogMapper.selectList(new LambdaQueryWrapper<AdminAuditLog>()
                .eq(AdminAuditLog::getOperatorId, admin.getId())
                .orderByDesc(AdminAuditLog::getCreatedAt)
                .orderByDesc(AdminAuditLog::getId));
        assertEquals(List.of(AdminAuditResult.FAILURE, AdminAuditResult.SUCCESS),
                records.stream().map(AdminAuditLog::getResult).toList());
        assertEquals(AdminAuditResourceType.TAG, records.getLast().getResourceType());
        assertEquals(AdminAuditAction.CREATE, records.getLast().getAction());
        assertNull(records.getFirst().getResourceId());
    }

    @Test
    void auditLogQueryShouldRequireAdminAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(101001));
    }

    private String accessToken() {
        return JwtUtil.generateAccessToken(
                signingKey,
                "sanjuu-blog",
                Duration.ofMinutes(15),
                admin.getId(),
                admin.getUsername(),
                UserRole.ADMIN.getValue(),
                UserStatus.ACTIVE.getValue()
        );
    }
}
