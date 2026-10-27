package com.ccsanjuu.blog.modules.friendlink;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccsanjuu.blog.common.util.JwtUtil;
import com.ccsanjuu.blog.modules.audit.mapper.AdminAuditLogMapper;
import com.ccsanjuu.blog.modules.audit.model.entity.AdminAuditLog;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditAction;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditResourceType;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditResult;
import com.ccsanjuu.blog.modules.friendlink.mapper.FriendLinkMapper;
import com.ccsanjuu.blog.modules.friendlink.model.entity.FriendLink;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FriendLinkIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecretKey signingKey;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private FriendLinkMapper friendLinkMapper;

    @Autowired
    private AdminAuditLogMapper adminAuditLogMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private User admin;
    private Long friendLinkId;

    @BeforeEach
    void setUp() {
        String suffix = Long.toString(System.nanoTime(), 36);
        admin = User.builder()
                .username("friend" + suffix)
                .nickname("friend" + suffix)
                .email("friend" + suffix + "@example.com")
                .passwordHash("integration-test-password")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .tokenVersion(0L)
                .avatarUrl("")
                .bio("")
                .emailVerified(false)
                .build();
        userMapper.insert(admin);
    }

    @AfterEach
    void tearDown() {
        if (admin != null && admin.getId() != null) {
            adminAuditLogMapper.delete(new LambdaQueryWrapper<AdminAuditLog>()
                    .eq(AdminAuditLog::getOperatorId, admin.getId()));
            stringRedisTemplate.delete("blog:auth:token-version:user:" + admin.getId());
            userMapper.deleteById(admin.getId());
        }
        if (friendLinkId != null) {
            friendLinkMapper.deleteById(friendLinkId);
        }
    }

    @Test
    void adminCanCreateFilterUpdateAndDeleteFriendLinkWithAudit() throws Exception {
        String name = "友链集成测试-" + System.nanoTime();
        mockMvc.perform(post("/api/v1/admin/friend-links")
                        .header("Authorization", "Bearer " + accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s", "url":"https://example.com/%s", "logoUrl":"",
                                 "description":"test", "section":"FRIENDS"}
                                """.formatted(name, name)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sortNo").value(100))
                .andExpect(jsonPath("$.data.status").value("ENABLED"));

        friendLinkId = friendLinkMapper.selectOne(new LambdaQueryWrapper<FriendLink>()
                .eq(FriendLink::getName, name)).getId();

        mockMvc.perform(put("/api/v1/admin/friend-links/{friendLinkId}", friendLinkId)
                        .header("Authorization", "Bearer " + accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s", "url":"https://example.com/%s", "logoUrl":"",
                                 "description":"updated", "section":"EXPERTS", "sortNo":250,
                                 "status":"DISABLED"}
                                """.formatted(name, name)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sortNo").value(250))
                .andExpect(jsonPath("$.data.status").value("DISABLED"));

        mockMvc.perform(get("/api/v1/admin/friend-links")
                        .header("Authorization", "Bearer " + accessToken())
                        .queryParam("keyword", name)
                        .queryParam("section", "EXPERTS")
                        .queryParam("status", "DISABLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].id").value(friendLinkId));

        mockMvc.perform(post("/api/v1/admin/friend-links")
                        .header("Authorization", "Bearer " + accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"duplicate-%s", "url":"https://example.com/%s", "section":"FRIENDS"}
                                """.formatted(name, name)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(109002));

        mockMvc.perform(delete("/api/v1/admin/friend-links/{friendLinkId}", friendLinkId)
                        .header("Authorization", "Bearer " + accessToken()))
                .andExpect(status().isOk());
        friendLinkId = null;

        List<AdminAuditLog> logs = adminAuditLogMapper.selectList(new LambdaQueryWrapper<AdminAuditLog>()
                .eq(AdminAuditLog::getOperatorId, admin.getId())
                .eq(AdminAuditLog::getResourceType, AdminAuditResourceType.FRIEND_LINK)
                .orderByAsc(AdminAuditLog::getId));
        assertEquals(List.of(
                        AdminAuditAction.CREATE,
                        AdminAuditAction.UPDATE,
                        AdminAuditAction.CREATE,
                        AdminAuditAction.DELETE
                ),
                logs.stream().map(AdminAuditLog::getAction).toList());
        assertEquals(List.of(
                        AdminAuditResult.SUCCESS,
                        AdminAuditResult.SUCCESS,
                        AdminAuditResult.FAILURE,
                        AdminAuditResult.SUCCESS
                ),
                logs.stream().map(AdminAuditLog::getResult).toList());
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
