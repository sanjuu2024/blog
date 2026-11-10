package com.ccsanjuu.blog.modules.category.controller;

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
import com.ccsanjuu.blog.modules.category.model.dto.AdminCategoryQueryDTO;
import com.ccsanjuu.blog.modules.category.model.dto.CategoryUpsertRequestDTO;
import com.ccsanjuu.blog.modules.category.model.enums.CategoryStatus;
import com.ccsanjuu.blog.modules.category.model.vo.AdminCategoryItemVO;
import com.ccsanjuu.blog.modules.category.model.vo.CreatedCategoryVO;
import com.ccsanjuu.blog.modules.category.model.vo.UpdatedCategoryVO;
import com.ccsanjuu.blog.modules.category.service.CategoryService;
import com.ccsanjuu.blog.modules.comment.mapper.CommentMapper;
import com.ccsanjuu.blog.modules.message.mapper.MessageMapper;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.time.Duration;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminCategoryController.class)
@Import({
        SecurityConfig.class,
        GlobalExceptionHandler.class,
        WebMvcConfig.class,
        AdminCategoryControllerTest.SecurityTestConfig.class
})
class AdminCategoryControllerTest {

    private static final Long ADMIN_ID = 10001L;
    private static final Long PARENT_CATEGORY_ID = 20001L;
    private static final Long CATEGORY_ID = 21001L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecretKey signingKey;

    @MockitoBean
    private CategoryService categoryService;

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
    private TagMapper tagMapper;

    @MockitoBean
    private UserMapper userMapper;

    @BeforeEach
    void setUpTokenVersion() {
        when(tokenVersionService.getCurrentVersion(anyLong())).thenReturn(0L);
    }

    @Test
    void categoryListShouldRejectUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get("/api/v1/admin/categories"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(101001));
    }

    @Test
    void categoryListShouldRejectNonAdminUser() throws Exception {
        mockMvc.perform(get("/api/v1/admin/categories")
                        .header("Authorization", "Bearer " + accessToken(UserRole.USER)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(101003));
    }

    @Test
    void categoryListShouldBindFiltersForAdmin() throws Exception {
        when(categoryService.getCategoryList(any(AdminCategoryQueryDTO.class))).thenReturn(List.of(
                AdminCategoryItemVO.builder()
                        .id(CATEGORY_ID)
                        .parentId(PARENT_CATEGORY_ID)
                        .level(2)
                        .name("Java")
                        .status(CategoryStatus.ENABLED)
                        .articleCount(8L)
                        .children(List.of())
                        .build()
        ));

        mockMvc.perform(get("/api/v1/admin/categories")
                        .header("Authorization", "Bearer " + accessToken(UserRole.ADMIN))
                        .queryParam("keyword", "Java")
                        .queryParam("status", "ENABLED")
                        .queryParam("level", "2")
                        .queryParam("parentId", PARENT_CATEGORY_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(CATEGORY_ID))
                .andExpect(jsonPath("$.data[0].articleCount").value(8));

        ArgumentCaptor<AdminCategoryQueryDTO> captor = ArgumentCaptor.forClass(AdminCategoryQueryDTO.class);
        verify(categoryService).getCategoryList(captor.capture());
        assertEquals("Java", captor.getValue().getKeyword());
        assertEquals(CategoryStatus.ENABLED, captor.getValue().getStatus());
        assertEquals(2, captor.getValue().getLevel());
        assertEquals(PARENT_CATEGORY_ID, captor.getValue().getParentId());
    }

    @Test
    void createCategoryShouldPassValidatedRequestToService() throws Exception {
        when(categoryService.createCategory(any(CategoryUpsertRequestDTO.class))).thenReturn(
                CreatedCategoryVO.builder()
                        .id(CATEGORY_ID)
                        .parentId(PARENT_CATEGORY_ID)
                        .level(2)
                        .name("Java")
                        .status(CategoryStatus.ENABLED)
                        .build()
        );

        mockMvc.perform(post("/api/v1/admin/categories")
                        .header("Authorization", "Bearer " + accessToken(UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(upsertRequestJson("Java")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(CATEGORY_ID))
                .andExpect(jsonPath("$.data.name").value("Java"));

        ArgumentCaptor<CategoryUpsertRequestDTO> captor = ArgumentCaptor.forClass(CategoryUpsertRequestDTO.class);
        verify(categoryService).createCategory(captor.capture());
        assertEquals(PARENT_CATEGORY_ID, captor.getValue().getParentId());
        assertEquals(2, captor.getValue().getLevel());
        assertEquals("Java", captor.getValue().getName());
    }

    @Test
    void updateCategoryShouldPassPathAndBodyToService() throws Exception {
        when(categoryService.updateCategory(eq(CATEGORY_ID), any(CategoryUpsertRequestDTO.class)))
                .thenReturn(UpdatedCategoryVO.builder()
                        .id(CATEGORY_ID)
                        .parentId(PARENT_CATEGORY_ID)
                        .level(2)
                        .name("Java SE")
                        .status(CategoryStatus.ENABLED)
                        .build());

        mockMvc.perform(put("/api/v1/admin/categories/{categoryId}", CATEGORY_ID)
                        .header("Authorization", "Bearer " + accessToken(UserRole.ADMIN))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(upsertRequestJson("Java SE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(CATEGORY_ID))
                .andExpect(jsonPath("$.data.name").value("Java SE"));

        verify(categoryService).updateCategory(eq(CATEGORY_ID), any(CategoryUpsertRequestDTO.class));
    }

    @Test
    void deleteCategoryShouldPassCategoryIdToService() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/categories/{categoryId}", CATEGORY_ID)
                        .header("Authorization", "Bearer " + accessToken(UserRole.ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(categoryService).deleteCategory(CATEGORY_ID);
    }

    private String upsertRequestJson(String name) {
        return """
                {
                  "parentId": 20001,
                  "level": 2,
                  "name": "%s",
                  "description": "Java articles",
                  "sortNo": 10,
                  "status": "ENABLED"
                }
                """.formatted(name);
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
