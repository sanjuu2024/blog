package com.ccsanjuu.blog.modules.category.service.impl;

import com.ccsanjuu.blog.modules.article.mapper.ArticleMapper;
import com.ccsanjuu.blog.modules.article.model.bo.CategoryArticleCountBO;
import com.ccsanjuu.blog.modules.category.mapper.CategoryMapper;
import com.ccsanjuu.blog.modules.category.model.entity.Category;
import com.ccsanjuu.blog.modules.category.model.enums.CategoryStatus;
import com.ccsanjuu.blog.modules.category.model.vo.PublicCategoryItemVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    private static final Long PARENT_CATEGORY_ID = 20001L;
    private static final Long EMPTY_PARENT_CATEGORY_ID = 20002L;
    private static final Long FIRST_CHILD_CATEGORY_ID = 21001L;
    private static final Long SECOND_CHILD_CATEGORY_ID = 21002L;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private ArticleMapper articleMapper;

    private CategoryServiceImpl categoryService;

    @BeforeEach
    void setUp() {
        categoryService = new CategoryServiceImpl(categoryMapper, articleMapper);
    }

    @Test
    void getEnabledCategoryListShouldBuildTreeAndAggregateVisibleArticleCounts() {
        when(categoryMapper.selectList(any())).thenReturn(List.of(
                parentCategory(PARENT_CATEGORY_ID, "Tech", 10),
                childCategory(FIRST_CHILD_CATEGORY_ID, PARENT_CATEGORY_ID, "Java", 11),
                childCategory(SECOND_CHILD_CATEGORY_ID, PARENT_CATEGORY_ID, "Algorithm", 12),
                parentCategory(EMPTY_PARENT_CATEGORY_ID, "Life", 20)
        ));
        when(articleMapper.getArticleCountByCategoryIds(List.of(FIRST_CHILD_CATEGORY_ID, SECOND_CHILD_CATEGORY_ID), true))
                .thenReturn(List.of(
                        CategoryArticleCountBO.builder()
                                .categoryId(FIRST_CHILD_CATEGORY_ID)
                                .articleCount(6L)
                                .build(),
                        CategoryArticleCountBO.builder()
                                .categoryId(SECOND_CHILD_CATEGORY_ID)
                                .articleCount(4L)
                                .build()
                ));

        List<PublicCategoryItemVO> result = categoryService.getEnabledCategoryList();

        assertEquals(2, result.size());
        PublicCategoryItemVO parent = result.getFirst();
        assertEquals(PARENT_CATEGORY_ID, parent.getId());
        assertEquals(10L, parent.getArticleCount());
        assertEquals(2, parent.getChildren().size());
        assertEquals(FIRST_CHILD_CATEGORY_ID, parent.getChildren().getFirst().getId());
        assertEquals(6L, parent.getChildren().getFirst().getArticleCount());
        assertTrue(parent.getChildren().getFirst().getChildren().isEmpty());
        assertEquals(SECOND_CHILD_CATEGORY_ID, parent.getChildren().get(1).getId());
        assertEquals(4L, parent.getChildren().get(1).getArticleCount());
        assertTrue(parent.getChildren().get(1).getChildren().isEmpty());

        PublicCategoryItemVO emptyParent = result.get(1);
        assertEquals(EMPTY_PARENT_CATEGORY_ID, emptyParent.getId());
        assertEquals(0L, emptyParent.getArticleCount());
        assertEquals(0, emptyParent.getChildren().size());
        verify(articleMapper).getArticleCountByCategoryIds(List.of(FIRST_CHILD_CATEGORY_ID, SECOND_CHILD_CATEGORY_ID), true);
    }

    @Test
    void getEnabledCategoryListShouldSkipArticleCountQueryWhenThereAreNoChildren() {
        when(categoryMapper.selectList(any())).thenReturn(List.of(
                parentCategory(PARENT_CATEGORY_ID, "Tech", 10)
        ));

        List<PublicCategoryItemVO> result = categoryService.getEnabledCategoryList();

        assertEquals(1, result.size());
        assertEquals(PARENT_CATEGORY_ID, result.getFirst().getId());
        assertEquals(0L, result.getFirst().getArticleCount());
        assertEquals(0, result.getFirst().getChildren().size());
        verifyNoInteractions(articleMapper);
    }

    private Category parentCategory(Long id, String name, Integer sortNo) {
        return Category.builder()
                .id(id)
                .name(name)
                .level(1)
                .sortNo(sortNo)
                .status(CategoryStatus.ENABLED)
                .build();
    }

    private Category childCategory(Long id, Long parentId, String name, Integer sortNo) {
        return Category.builder()
                .id(id)
                .parentId(parentId)
                .name(name)
                .level(2)
                .sortNo(sortNo)
                .status(CategoryStatus.ENABLED)
                .build();
    }
}
