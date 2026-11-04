package com.ccsanjuu.blog.modules.mail;

import com.ccsanjuu.blog.modules.mail.model.dto.MailDeliveryQueryDTO;
import com.ccsanjuu.blog.modules.mail.service.MailDeliveryService;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import com.ccsanjuu.blog.modules.user.model.enums.UserRole;
import com.ccsanjuu.blog.modules.user.model.enums.UserStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MailDeliveryIntegrationTest {

    @Autowired
    private MailDeliveryService service;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    private final List<Long> deliveryIds = new ArrayList<>();
    private final List<Long> userIds = new ArrayList<>();

    @AfterEach
    void clearCommittedFixtures() {
        deliveryIds.forEach(id -> jdbc.update("DELETE FROM blog_mail_delivery WHERE id = ?", id));
        userIds.forEach(id -> jdbc.update("DELETE FROM blog_user WHERE id = ?", id));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldExposeFullAddressAndCurrentUserWithoutRewritingHistoricalRecipient() throws Exception {
        String username = "mail_" + UUID.randomUUID().toString().substring(0, 8);
        User user = User.builder().username(username).nickname("原昵称").email(username + "@example.com")
                .passwordHash("test-password-hash").role(UserRole.USER).status(UserStatus.ACTIVE)
                .tokenVersion(0L).avatarUrl("").bio("").emailVerified(true).build();
        userMapper.insert(user);
        userIds.add(user.getId());
        var delivery = service.createPending("COMMENT_REPLY", 60001L, 60002L, "original@example.com", user.getId());
        deliveryIds.add(delivery.getId());
        user.setNickname("当前昵称");
        user.setEmail("changed-" + username + "@example.com");
        userMapper.updateById(user);

        mockMvc.perform(get("/api/v1/admin/mail-deliveries/{id}", delivery.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recipientEmail").value("original@example.com"))
                .andExpect(jsonPath("$.data.userId").value(user.getId()))
                .andExpect(jsonPath("$.data.userNickname").value("当前昵称"))
                .andExpect(jsonPath("$.data.currentRecipientEmail").value(user.getEmail()))
                .andExpect(jsonPath("$.data.recipientMasked").doesNotExist());
        var query = new MailDeliveryQueryDTO();
        query.setPageSize(100);
        assertTrue(service.getPage(query).getRecords().stream()
                .anyMatch(item -> item.getId().equals(delivery.getId()) && "当前昵称".equals(item.getUserNickname())));

        user.setDeletedAt(OffsetDateTime.now());
        user.setUsername("deleted_" + user.getId());
        userMapper.updateById(user);
        var detail = service.getDetail(delivery.getId());
        assertTrue(detail.getUserDeleted());
        assertNull(detail.getUserNickname());
        assertEquals(user.getUsername(), detail.getUserUsername());
        assertNull(detail.getCurrentRecipientEmail());
        assertEquals("original@example.com", detail.getRecipientEmail());
        service.updateRecipient(delivery.getId(), "retry@example.com");
        assertEquals("retry@example.com", service.getDetail(delivery.getId()).getRecipientEmail());
    }

    @Test
    void shouldNotAssociateGuestsByEmailOrInventMissingHistoricalAddresses() {
        var guest = service.createPending("MESSAGE_REPLY", 90001L, 90002L, "guest@example.com", null);
        deliveryIds.add(guest.getId());
        assertNull(service.getDetail(guest.getId()).getUserId());
        var legacy = service.createPending("MESSAGE_REPLY", 90003L, 90004L, null, null);
        deliveryIds.add(legacy.getId());
        jdbc.update("UPDATE blog_mail_delivery SET recipient_masked = ? WHERE id = ?", "g***@example.com", legacy.getId());
        assertNull(service.getDetail(legacy.getId()).getRecipientEmail());
        assertEquals("g***@example.com", service.getDetail(legacy.getId()).getRecipientMasked());
    }

    @Test
    @WithMockUser(roles = "USER")
    void ordinaryUsersShouldNotReadMailRecipientDetails() throws Exception {
        mockMvc.perform(get("/api/v1/admin/mail-deliveries")).andExpect(status().isForbidden());
    }
}
