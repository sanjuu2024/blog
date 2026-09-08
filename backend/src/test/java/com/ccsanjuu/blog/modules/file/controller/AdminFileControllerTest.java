package com.ccsanjuu.blog.modules.file.controller;

import com.ccsanjuu.blog.common.exception.GlobalExceptionHandler;
import com.ccsanjuu.blog.common.util.JwtUtil;
import com.ccsanjuu.blog.config.SecurityConfig;
import com.ccsanjuu.blog.config.WebMvcConfig;
import com.ccsanjuu.blog.modules.article.mapper.ArticleMapper;
import com.ccsanjuu.blog.modules.article.mapper.ArticleTagMapper;
import com.ccsanjuu.blog.modules.auth.mapper.AuthMapper;
import com.ccsanjuu.blog.modules.auth.service.TokenVersionService;
import com.ccsanjuu.blog.modules.category.mapper.CategoryMapper;
import com.ccsanjuu.blog.modules.comment.mapper.CommentMapper;
import com.ccsanjuu.blog.modules.message.mapper.MessageMapper;
import com.ccsanjuu.blog.modules.file.model.enums.AdminImageUploadScene;
import com.ccsanjuu.blog.modules.file.model.vo.UploadedImageVO;
import com.ccsanjuu.blog.modules.file.service.ImageUploadService;
import com.ccsanjuu.blog.modules.audit.mapper.AdminAuditLogMapper;
import com.ccsanjuu.blog.modules.tag.mapper.TagMapper;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.enums.UserRole;
import com.ccsanjuu.blog.modules.user.model.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.time.Duration;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminFileController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        WebMvcConfig.class,
        AdminFileControllerTest.SecurityTestConfig.class
})
class AdminFileControllerTest {

    private static final Long ADMIN_ID = 10001L;
    private static final Long USER_ID = 10002L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecretKey signingKey;

    @MockitoBean
    private ImageUploadService imageUploadService;

    @MockitoBean
    private AdminAuditLogMapper adminAuditLogMapper;

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
    private MessageMapper messageMapper;

    @MockitoBean
    private TagMapper tagMapper;

    @MockitoBean
    private UserMapper userMapper;

    @BeforeEach
    void setUpTokenVersion() {
        when(tokenVersionService.getCurrentVersion(anyLong())).thenReturn(0L);
    }

    @Test
    void uploadImageShouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(multipart("/api/v1/admin/files/images")
                        .file(imageFile())
                        .param("scene", "ARTICLE_COVER"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(101001));
    }

    @Test
    void uploadImageShouldRejectNonAdminUser() throws Exception {
        mockMvc.perform(multipart("/api/v1/admin/files/images")
                        .file(imageFile())
                        .param("scene", "ARTICLE_COVER")
                        .header("Authorization", "Bearer " + accessToken(USER_ID, UserRole.USER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(101003));
    }

    @Test
    void uploadImageShouldReturnUploadedImageForAdmin() throws Exception {
        MockMultipartFile file = imageFile();
        when(imageUploadService.uploadAdminImage(eq(AdminImageUploadScene.ARTICLE_COVER), eq(file)))
                .thenReturn(UploadedImageVO.builder()
                        .url("https://img.example.com/cover.png")
                        .originalName("cover.png")
                        .contentType("image/png")
                        .size(8L)
                        .build());

        mockMvc.perform(multipart("/api/v1/admin/files/images")
                        .file(file)
                        .param("scene", "ARTICLE_COVER")
                        .header("Authorization", "Bearer " + accessToken(ADMIN_ID, UserRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value("https://img.example.com/cover.png"))
                .andExpect(jsonPath("$.data.originalName").value("cover.png"))
                .andExpect(jsonPath("$.data.contentType").value("image/png"))
                .andExpect(jsonPath("$.data.size").value(8));

        verify(imageUploadService).uploadAdminImage(AdminImageUploadScene.ARTICLE_COVER, file);
    }

    @Test
    void uploadImageShouldRejectInvalidScene() throws Exception {
        mockMvc.perform(multipart("/api/v1/admin/files/images")
                        .file(imageFile())
                        .param("scene", "UNKNOWN")
                        .header("Authorization", "Bearer " + accessToken(ADMIN_ID, UserRole.ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(199001));
    }

    @Test
    void uploadImageShouldReturnImageRequiredWhenFilePartIsMissing() throws Exception {
        mockMvc.perform(multipart("/api/v1/admin/files/images")
                        .param("scene", "ARTICLE_COVER")
                        .header("Authorization", "Bearer " + accessToken(ADMIN_ID, UserRole.ADMIN)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(107001));
    }

    private MockMultipartFile imageFile() {
        return new MockMultipartFile(
                "file",
                "cover.png",
                "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}
        );
    }

    private String accessToken(Long userId, UserRole role) {
        return JwtUtil.generateAccessToken(
                signingKey,
                "sanjuu-blog",
                Duration.ofMinutes(15),
                userId,
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
