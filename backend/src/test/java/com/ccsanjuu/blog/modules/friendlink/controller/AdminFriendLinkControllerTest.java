package com.ccsanjuu.blog.modules.friendlink.controller;

import com.ccsanjuu.blog.common.exception.GlobalExceptionHandler;
import com.ccsanjuu.blog.common.util.JwtUtil;
import com.ccsanjuu.blog.config.SecurityConfig;
import com.ccsanjuu.blog.config.WebMvcConfig;
import com.ccsanjuu.blog.modules.audit.mapper.AdminAuditLogMapper;
import com.ccsanjuu.blog.modules.auth.mapper.AuthMapper;
import com.ccsanjuu.blog.modules.auth.service.TokenVersionService;
import com.ccsanjuu.blog.modules.friendlink.model.dto.AdminFriendLinkQueryDTO;
import com.ccsanjuu.blog.modules.friendlink.model.dto.FriendLinkUpsertRequestDTO;
import com.ccsanjuu.blog.modules.friendlink.model.dto.FriendLinkUpdateRequestDTO;
import com.ccsanjuu.blog.modules.friendlink.model.enums.FriendLinkSection;
import com.ccsanjuu.blog.modules.friendlink.model.enums.FriendLinkStatus;
import com.ccsanjuu.blog.modules.friendlink.model.vo.AdminFriendLinkItemVO;
import com.ccsanjuu.blog.modules.friendlink.model.vo.FriendLinkMutationVO;
import com.ccsanjuu.blog.modules.friendlink.model.vo.PublicFriendLinkItemVO;
import com.ccsanjuu.blog.modules.friendlink.model.vo.PublicFriendLinkSectionVO;
import com.ccsanjuu.blog.modules.friendlink.service.FriendLinkService;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.enums.UserRole;
import com.ccsanjuu.blog.modules.user.model.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AdminFriendLinkController.class, FriendLinkController.class})
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        WebMvcConfig.class,
        AdminFriendLinkControllerTest.SecurityTestConfig.class
})
class AdminFriendLinkControllerTest {

    private static final Long ADMIN_ID = 10001L;
    private static final Long FRIEND_LINK_ID = 110001L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecretKey signingKey;

    @MockitoBean
    private FriendLinkService friendLinkService;

    @MockitoBean
    private TokenVersionService tokenVersionService;

    @MockitoBean
    private AuthMapper authMapper;

    @MockitoBean
    private AdminAuditLogMapper adminAuditLogMapper;

    @MockitoBean
    private UserMapper userMapper;

    @BeforeEach
    void setUpTokenVersion() {
        when(tokenVersionService.getCurrentVersion(anyLong())).thenReturn(0L);
    }

    @Test
    void endpointShouldRequireAdminRole() throws Exception {
        mockMvc.perform(get("/api/v1/admin/friend-links")
                        .header("Authorization", "Bearer " + accessToken(UserRole.USER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(101003));
    }

    @Test
    void publicFriendLinkListShouldAllowAnonymousRequestsAndUseNoStore() throws Exception {
        when(friendLinkService.getPublicFriendLinkSections()).thenReturn(List.of(
                PublicFriendLinkSectionVO.builder()
                        .section(FriendLinkSection.FRIENDS)
                        .name("友人帐")
                        .records(List.of(PublicFriendLinkItemVO.builder()
                                .id(FRIEND_LINK_ID)
                                .name("Example")
                                .url("https://example.com")
                                .logoUrl("")
                                .description("Example site")
                                .section(FriendLinkSection.FRIENDS)
                                .sortNo(100)
                                .build()))
                        .build(),
                PublicFriendLinkSectionVO.builder().section(FriendLinkSection.EXPERTS).name("大佬们")
                        .records(List.of()).build(),
                PublicFriendLinkSectionVO.builder().section(FriendLinkSection.PEERS).name("同行者")
                        .records(List.of()).build()
        ));

        mockMvc.perform(get("/api/v1/friend-links"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].name").value("友人帐"))
                .andExpect(jsonPath("$.data[0].records[0].id").value(FRIEND_LINK_ID))
                .andExpect(jsonPath("$.data[0].records[0].status").doesNotExist())
                .andExpect(jsonPath("$.data[1].records.length()").value(0))
                .andExpect(jsonPath("$.data[2].name").value("同行者"));

        verify(friendLinkService).getPublicFriendLinkSections();
    }

    @Test
    void listShouldBindFiltersAndReturnPage() throws Exception {
        when(friendLinkService.getAdminFriendLinkList(any(AdminFriendLinkQueryDTO.class))).thenReturn(
                com.ccsanjuu.blog.common.api.PageResult.of(1, 1, 10, List.of(item())));

        mockMvc.perform(get("/api/v1/admin/friend-links")
                        .header("Authorization", "Bearer " + accessToken(UserRole.ADMIN))
                        .queryParam("pageNum", "2")
                        .queryParam("pageSize", "20")
                        .queryParam("keyword", "example")
                        .queryParam("section", "FRIENDS")
                        .queryParam("status", "ENABLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].id").value(FRIEND_LINK_ID));

        ArgumentCaptor<AdminFriendLinkQueryDTO> captor = ArgumentCaptor.forClass(AdminFriendLinkQueryDTO.class);
        verify(friendLinkService).getAdminFriendLinkList(captor.capture());
        assertEquals(2, captor.getValue().getPageNum());
        assertEquals(20, captor.getValue().getPageSize());
        assertEquals(FriendLinkSection.FRIENDS, captor.getValue().getSection());
        assertEquals(FriendLinkStatus.ENABLED, captor.getValue().getStatus());
    }

    @Test
    void createShouldValidateAndDelegateRequest() throws Exception {
        when(friendLinkService.createFriendLink(any(FriendLinkUpsertRequestDTO.class))).thenReturn(mutation());

        mockMvc.perform(post("/api/v1/admin/friend-links")
                        .header("Authorization", "Bearer " + accessToken(UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("https://example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(FRIEND_LINK_ID))
                .andExpect(jsonPath("$.data.sortNo").value(100));

        ArgumentCaptor<FriendLinkUpsertRequestDTO> captor = ArgumentCaptor.forClass(FriendLinkUpsertRequestDTO.class);
        verify(friendLinkService).createFriendLink(captor.capture());
        assertEquals(FriendLinkSection.FRIENDS, captor.getValue().getSection());
        assertEquals(100, captor.getValue().getSortNo());
    }

    @Test
    void createShouldRejectInvalidFields() throws Exception {
        mockMvc.perform(post("/api/v1/admin/friend-links")
                        .header("Authorization", "Bearer " + accessToken(UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"", "url":"https://example.com", "section":"FRIENDS"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(199001));
    }

    @Test
    void updateShouldRequireStatusAndSortNo() throws Exception {
        mockMvc.perform(put("/api/v1/admin/friend-links/{friendLinkId}", FRIEND_LINK_ID)
                        .header("Authorization", "Bearer " + accessToken(UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Example", "url":"https://example.com", "section":"FRIENDS"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(199001));
    }

    @Test
    void updateAndDeleteShouldPassIdToService() throws Exception {
        when(friendLinkService.updateFriendLink(eq(FRIEND_LINK_ID), any(FriendLinkUpdateRequestDTO.class)))
                .thenReturn(mutation());

        mockMvc.perform(put("/api/v1/admin/friend-links/{friendLinkId}", FRIEND_LINK_ID)
                        .header("Authorization", "Bearer " + accessToken(UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson("https://example.com")))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/admin/friend-links/{friendLinkId}", FRIEND_LINK_ID)
                        .header("Authorization", "Bearer " + accessToken(UserRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());

        verify(friendLinkService).updateFriendLink(eq(FRIEND_LINK_ID), any(FriendLinkUpdateRequestDTO.class));
        verify(friendLinkService).deleteFriendLink(FRIEND_LINK_ID);
    }

    private AdminFriendLinkItemVO item() {
        return AdminFriendLinkItemVO.builder()
                .id(FRIEND_LINK_ID)
                .name("Example")
                .url("https://example.com")
                .logoUrl("")
                .description("Example site")
                .section(FriendLinkSection.FRIENDS)
                .sortNo(100)
                .status(FriendLinkStatus.ENABLED)
                .createdAt(OffsetDateTime.parse("2026-10-26T00:00:00Z"))
                .updatedAt(OffsetDateTime.parse("2026-10-26T00:00:00Z"))
                .build();
    }

    private FriendLinkMutationVO mutation() {
        return FriendLinkMutationVO.builder()
                .id(FRIEND_LINK_ID)
                .name("Example")
                .url("https://example.com")
                .logoUrl("")
                .description("Example site")
                .section(FriendLinkSection.FRIENDS)
                .sortNo(100)
                .status(FriendLinkStatus.ENABLED)
                .createdAt(OffsetDateTime.parse("2026-10-26T00:00:00Z"))
                .updatedAt(OffsetDateTime.parse("2026-10-26T00:00:00Z"))
                .build();
    }

    private String requestJson(String url) {
        return """
                {"name":"Example", "url":"%s", "logoUrl":"", "description":"Example site",
                  "section":"FRIENDS", "sortNo":100, "status":"ENABLED"}
                """.formatted(url);
    }

    private String accessToken(UserRole role) {
        return JwtUtil.generateAccessToken(
                signingKey,
                "sanjuu-blog",
                Duration.ofMinutes(15),
                ADMIN_ID,
                "tester",
                role.getValue(),
                UserStatus.ACTIVE.getValue()
        );
    }

    @TestConfiguration
    static class SecurityTestConfig {

        @Bean
        SecretKey jwtSigningKey() {
            return JwtUtil.createHmacShaKey("0123456789abcdef0123456789abcdef");
        }
    }
}
