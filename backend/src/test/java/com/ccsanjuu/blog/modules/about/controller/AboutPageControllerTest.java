package com.ccsanjuu.blog.modules.about.controller;

import com.ccsanjuu.blog.common.exception.GlobalExceptionHandler;
import com.ccsanjuu.blog.common.util.JwtUtil;
import com.ccsanjuu.blog.config.SecurityConfig;
import com.ccsanjuu.blog.config.WebMvcConfig;
import com.ccsanjuu.blog.modules.article.mapper.ArticleMapper;
import com.ccsanjuu.blog.modules.article.mapper.ArticleTagMapper;
import com.ccsanjuu.blog.modules.audit.mapper.AdminAuditLogMapper;
import com.ccsanjuu.blog.modules.auth.mapper.AuthMapper;
import com.ccsanjuu.blog.modules.auth.service.TokenVersionService;
import com.ccsanjuu.blog.modules.category.mapper.CategoryMapper;
import com.ccsanjuu.blog.modules.comment.mapper.CommentMapper;
import com.ccsanjuu.blog.modules.message.mapper.MessageMapper;
import com.ccsanjuu.blog.modules.about.mapper.AboutPageMapper;
import com.ccsanjuu.blog.modules.about.model.dto.UpdateAboutPageRequestDTO;
import com.ccsanjuu.blog.modules.about.model.vo.AdminAboutPageVO;
import com.ccsanjuu.blog.modules.about.model.vo.PublicAboutPageVO;
import com.ccsanjuu.blog.modules.about.service.AboutPageService;
import com.ccsanjuu.blog.modules.privacy.controller.PrivacyPolicyController;
import com.ccsanjuu.blog.modules.privacy.model.vo.PrivacyPolicyVO;
import com.ccsanjuu.blog.modules.privacy.service.PrivacyPolicyService;
import com.ccsanjuu.blog.modules.tag.mapper.TagMapper;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AboutPageController.class,
        AdminAboutPageController.class,
        PrivacyPolicyController.class
})
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        WebMvcConfig.class,
        AboutPageControllerTest.SecurityTestConfig.class
})
class AboutPageControllerTest {

    private static final Long ADMIN_ID = 10001L;
    private static final String POLICY_VERSION =
            "sha256:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecretKey signingKey;

    @MockitoBean
    private AboutPageService aboutPageService;

    @MockitoBean
    private PrivacyPolicyService privacyPolicyService;

    @MockitoBean
    private TokenVersionService tokenVersionService;

    @MockitoBean
    private ArticleMapper articleMapper;

    @MockitoBean
    private ArticleTagMapper articleTagMapper;

    @MockitoBean
    private AuthMapper authMapper;

    @MockitoBean
    private CategoryMapper categoryMapper;

    @MockitoBean
    private CommentMapper commentMapper;

    @MockitoBean
    private AdminAuditLogMapper adminAuditLogMapper;

    @MockitoBean
    private MessageMapper messageMapper;

    @MockitoBean
    private AboutPageMapper aboutPageMapper;

    @MockitoBean
    private TagMapper tagMapper;

    @MockitoBean
    private UserMapper userMapper;

    @BeforeEach
    void setUpTokenVersion() {
        when(tokenVersionService.getCurrentVersion(anyLong())).thenReturn(0L);
    }

    @Test
    void publicAboutPageShouldBeAccessibleWithoutAuthentication() throws Exception {
        when(aboutPageService.getPublicAboutPage()).thenReturn(PublicAboutPageVO.builder()
                .exists(false)
                .contentHtml("<p>暂无内容</p>")
                .contentText("暂无内容")
                .build());

        mockMvc.perform(get("/api/v1/about"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exists").value(false))
                .andExpect(jsonPath("$.data.contentText").value("暂无内容"));
    }

    @Test
    void privacyPolicyShouldReturnEtagAndSupportNotModified() throws Exception {
        when(privacyPolicyService.getPrivacyPolicy()).thenReturn(PrivacyPolicyVO.builder()
                .version(POLICY_VERSION)
                .contentHtml("<h1>隐私政策</h1>")
                .contentText("隐私政策")
                .build());

        mockMvc.perform(get("/api/v1/privacy-policy"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ETAG, '"' + POLICY_VERSION + '"'))
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-cache"))
                .andExpect(jsonPath("$.data.version").value(POLICY_VERSION));

        mockMvc.perform(get("/api/v1/privacy-policy")
                        .header(HttpHeaders.IF_NONE_MATCH, '"' + POLICY_VERSION + '"'))
                .andExpect(status().isNotModified())
                .andExpect(jsonPath("$").doesNotExist());
    }

    @Test
    void adminAboutPageShouldRequireAdministrator() throws Exception {
        mockMvc.perform(get("/api/v1/admin/about"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/admin/about")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(UserRole.USER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void administratorShouldReadAboutPageEditor() throws Exception {
        when(aboutPageService.getAdminAboutPage()).thenReturn(AdminAboutPageVO.builder()
                .exists(false)
                .contentMd("")
                .contentHtml("<p>暂无内容</p>")
                .contentText("暂无内容")
                .build());

        mockMvc.perform(get("/api/v1/admin/about")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(UserRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.exists").value(false))
                .andExpect(jsonPath("$.data.contentMd").value(""));
    }

    @Test
    void adminShouldSaveValidatedAboutPage() throws Exception {
        when(aboutPageService.upsertAboutPage(eq(ADMIN_ID), any(UpdateAboutPageRequestDTO.class)))
                .thenReturn(AdminAboutPageVO.builder()
                        .exists(true)
                        .contentMd("# 关于本站")
                        .contentHtml("<h1>关于本站</h1>")
                        .contentText("关于本站")
                        .build());

        mockMvc.perform(put("/api/v1/admin/about")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentMd":"# 关于本站"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contentMd").value("# 关于本站"));

        ArgumentCaptor<UpdateAboutPageRequestDTO> captor =
                ArgumentCaptor.forClass(UpdateAboutPageRequestDTO.class);
        verify(aboutPageService).upsertAboutPage(eq(ADMIN_ID), captor.capture());
        assertEquals("# 关于本站", captor.getValue().getContentMd());
    }

    @Test
    void adminAboutPageShouldRejectBlankMarkdown() throws Exception {
        mockMvc.perform(put("/api/v1/admin/about")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contentMd":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(199001));
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
