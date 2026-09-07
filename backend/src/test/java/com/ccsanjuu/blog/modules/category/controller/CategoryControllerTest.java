package com.ccsanjuu.blog.modules.category.controller;

import com.ccsanjuu.blog.common.exception.GlobalExceptionHandler;
import com.ccsanjuu.blog.config.WebMvcConfig;
import com.ccsanjuu.blog.modules.article.mapper.ArticleMapper;
import com.ccsanjuu.blog.modules.article.mapper.ArticleTagMapper;
import com.ccsanjuu.blog.modules.auth.mapper.AuthMapper;
import com.ccsanjuu.blog.modules.category.mapper.CategoryMapper;
import com.ccsanjuu.blog.modules.category.model.vo.PublicCategoryItemVO;
import com.ccsanjuu.blog.modules.category.service.CategoryService;
import com.ccsanjuu.blog.modules.comment.mapper.CommentMapper;
import com.ccsanjuu.blog.modules.message.mapper.MessageMapper;
import com.ccsanjuu.blog.modules.tag.mapper.TagMapper;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({
        GlobalExceptionHandler.class,
        WebMvcConfig.class
})
class CategoryControllerTest {

    private static final Long PARENT_CATEGORY_ID = 20001L;
    private static final Long CHILD_CATEGORY_ID = 21001L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private AuthMapper authMapper;

    @MockitoBean
    private UserMapper userMapper;

    @MockitoBean
    private CategoryMapper categoryMapper;

    @MockitoBean
    private CommentMapper commentMapper;

    @MockitoBean
    private MessageMapper messageMapper;

    @MockitoBean
    private TagMapper tagMapper;

    @MockitoBean
    private ArticleMapper articleMapper;

    @MockitoBean
    private ArticleTagMapper articleTagMapper;

    @Test
    void getEnabledCategoryListShouldCallService() throws Exception {
        when(categoryService.getEnabledCategoryList()).thenReturn(List.of(PublicCategoryItemVO.builder()
                .id(PARENT_CATEGORY_ID)
                .name("Tech")
                .level(1)
                .articleCount(6L)
                .children(List.of(PublicCategoryItemVO.builder()
                        .id(CHILD_CATEGORY_ID)
                        .name("Java")
                        .level(2)
                        .articleCount(6L)
                        .children(List.of())
                        .build()))
                .build()));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(PARENT_CATEGORY_ID))
                .andExpect(jsonPath("$.data[0].children[0].id").value(CHILD_CATEGORY_ID));

        verify(categoryService).getEnabledCategoryList();
    }
}
