package com.ccsanjuu.blog.modules.security;

import com.ccsanjuu.blog.modules.security.mapper.SecurityEventMapper;
import com.ccsanjuu.blog.modules.security.model.bo.SecurityEventRecordBO;
import com.ccsanjuu.blog.modules.security.model.entity.SecurityEvent;
import com.ccsanjuu.blog.modules.security.model.dto.SecurityEventQueryDTO;
import com.ccsanjuu.blog.modules.security.model.enums.SecurityEventOutcome;
import com.ccsanjuu.blog.modules.security.model.enums.SecurityEventType;
import com.ccsanjuu.blog.modules.security.service.SecurityEventService;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import com.ccsanjuu.blog.modules.user.model.enums.UserRole;
import com.ccsanjuu.blog.modules.user.model.enums.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest
class SecurityEventIntegrationTest {

    @Autowired
    private SecurityEventService securityEventService;

    @Autowired
    private SecurityEventMapper securityEventMapper;

    @Autowired
    private UserMapper userMapper;

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void eventShouldBePersistedWithReadableSourceAndFilledCreatedAt() {
        String account = "integration-" + UUID.randomUUID() + "@example.com";
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("2001:db8::1");
        request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/130.0.0.0");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        securityEventService.record(new SecurityEventRecordBO(
                SecurityEventType.LOGIN,
                SecurityEventOutcome.FAILURE,
                10001L,
                null,
                account,
                "密码错误"
        ));

        SecurityEvent event = securityEventMapper.selectList(null).stream()
                .filter(item -> account.equals(item.getAccount()))
                .findFirst()
                .orElseThrow();
        try {
            assertEquals(SecurityEventOutcome.FAILURE, event.getOutcome());
            assertNotNull(event.getCreatedAt());
            assertEquals("2001:db8::1", event.getIp());
            assertEquals("POST", event.getRequestMethod());
            assertEquals("/api/v1/auth/login", event.getRequestPath());
            SecurityEventQueryDTO query = new SecurityEventQueryDTO();
            query.setUserId(10001L);
            var item = securityEventService.getSecurityEventList(query).getRecords().stream()
                    .filter(record -> event.getId().equals(record.getId())).findFirst().orElseThrow();
            assertEquals(account, item.getAccount());
            assertEquals("Chrome", item.getBrowser());
            assertNull(item.getActorId());
        } finally {
            securityEventMapper.deleteById(event.getId());
        }
    }

    @Test
    @Transactional
    void queryShouldReturnCurrentProfilesAndDeletedUsernames() {
        User target = createUser(UserRole.USER, "测试用户");
        User actor = createUser(UserRole.ADMIN, "站点管理员");
        SecurityEvent event = SecurityEvent.builder()
                .eventType(SecurityEventType.USER_STATUS_CHANGE)
                .outcome(SecurityEventOutcome.SUCCESS)
                .userId(target.getId())
                .actorId(actor.getId())
                .description("用户状态修改为 DISABLED")
                .build();
        securityEventMapper.insert(event);
        SecurityEventQueryDTO query = new SecurityEventQueryDTO();
        query.setUserId(target.getId());

        var item = securityEventService.getSecurityEventList(query).getRecords().getFirst();

        assertEquals(target.getUsername(), item.getUserUsername());
        assertEquals("测试用户", item.getUserNickname());
        assertEquals(actor.getUsername(), item.getActorUsername());
        assertEquals("站点管理员", item.getActorNickname());

        userMapper.updateById(User.builder().id(target.getId()).nickname("新昵称").build());
        assertEquals("新昵称", securityEventService.getSecurityEventList(query).getRecords().getFirst().getUserNickname());

        userMapper.updateById(User.builder().id(target.getId())
                .username("deleted_" + target.getId()).deletedAt(OffsetDateTime.now(ZoneOffset.UTC)).build());
        userMapper.updateById(User.builder().id(actor.getId())
                .username("deleted_" + actor.getId()).deletedAt(OffsetDateTime.now(ZoneOffset.UTC)).build());
        var deletedItem = securityEventService.getSecurityEventList(query).getRecords().getFirst();

        assertEquals(target.getId(), deletedItem.getUserId());
        assertTrue(deletedItem.getUserDeleted());
        assertEquals("deleted_" + target.getId(), deletedItem.getUserUsername());
        assertNull(deletedItem.getUserNickname());
        assertEquals(actor.getId(), deletedItem.getActorId());
        assertTrue(deletedItem.getActorDeleted());
        assertEquals("deleted_" + actor.getId(), deletedItem.getActorUsername());
        assertNull(deletedItem.getActorNickname());
    }

    private User createUser(UserRole role, String nickname) {
        String username = "sec_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        User user = User.builder().username(username).nickname(nickname)
                .email(username + "@example.com").passwordHash("test-password-hash")
                .role(role).status(UserStatus.ACTIVE).tokenVersion(0L)
                .emailVerified(true).avatarUrl("").build();
        userMapper.insert(user);
        return user;
    }
}
