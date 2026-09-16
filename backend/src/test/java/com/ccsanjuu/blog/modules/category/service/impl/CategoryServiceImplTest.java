package com.ccsanjuu.blog.modules.category.service.impl;

import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.article.mapper.ArticleMapper;
import com.ccsanjuu.blog.modules.article.model.bo.CategoryArticleCountBO;
import com.ccsanjuu.blog.modules.category.mapper.CategoryMapper;
import com.ccsanjuu.blog.modules.category.model.dto.AdminCategoryQueryDTO;
import com.ccsanjuu.blog.modules.category.model.dto.CategoryUpsertRequestDTO;
import com.ccsanjuu.blog.modules.category.model.entity.Category;
import com.ccsanjuu.blog.modules.category.model.enums.CategoryStatus;
import com.ccsanjuu.blog.modules.category.model.vo.AdminCategoryItemVO;
import com.ccsanjuu.blog.modules.category.model.vo.CreatedCategoryVO;
import com.ccsanjuu.blog.modules.category.model.vo.PublicCategoryItemVO;
import com.ccsanjuu.blog.modules.category.model.vo.UpdatedCategoryVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    void getCategoryListShouldBuildTreeAndAggregateAllArticleCounts() {
        List<Category> categories = List.of(
                parentCategory(PARENT_CATEGORY_ID, "Tech", 10),
                childCategory(FIRST_CHILD_CATEGORY_ID, PARENT_CATEGORY_ID, "Java", 11),
                childCategory(SECOND_CHILD_CATEGORY_ID, PARENT_CATEGORY_ID, "Algorithm", 12),
                parentCategory(EMPTY_PARENT_CATEGORY_ID, "Life", 20)
        );
        when(categoryMapper.selectList(any()))
                .thenReturn(categories)
                .thenReturn(categories.subList(1, 3));
        when(articleMapper.getArticleCountByCategoryIds(
                List.of(FIRST_CHILD_CATEGORY_ID, SECOND_CHILD_CATEGORY_ID),
                false
        )).thenReturn(List.of(CategoryArticleCountBO.builder()
                .categoryId(FIRST_CHILD_CATEGORY_ID)
                .articleCount(3L)
                .build()));

        List<AdminCategoryItemVO> result = categoryService.getCategoryList(new AdminCategoryQueryDTO());

        assertEquals(2, result.size());
        assertEquals(3L, result.getFirst().getArticleCount());
        assertEquals(2, result.getFirst().getChildren().size());
        assertEquals(3L, result.getFirst().getChildren().getFirst().getArticleCount());
        assertEquals(0L, result.getFirst().getChildren().get(1).getArticleCount());
        assertTrue(result.get(1).getChildren().isEmpty());
        assertEquals(0L, result.get(1).getArticleCount());
    }

    @Test
    void getCategoryListShouldReturnFlatKeywordResult() {
        AdminCategoryQueryDTO queryDTO = AdminCategoryQueryDTO.builder()
                .keyword(" JAVA ")
                .build();
        when(categoryMapper.selectList(any())).thenReturn(List.of(
                childCategory(FIRST_CHILD_CATEGORY_ID, PARENT_CATEGORY_ID, "Java", 11)
        ));
        when(articleMapper.getArticleCountByCategoryIds(List.of(FIRST_CHILD_CATEGORY_ID), false))
                .thenReturn(List.of(CategoryArticleCountBO.builder()
                        .categoryId(FIRST_CHILD_CATEGORY_ID)
                        .articleCount(5L)
                        .build()));

        List<AdminCategoryItemVO> result = categoryService.getCategoryList(queryDTO);

        assertEquals(1, result.size());
        assertEquals(FIRST_CHILD_CATEGORY_ID, result.getFirst().getId());
        assertEquals(5L, result.getFirst().getArticleCount());
        assertTrue(result.getFirst().getChildren().isEmpty());
        verify(categoryMapper, times(1)).selectList(any());
    }

    @Test
    void getCategoryListShouldRejectParentIdCombinedWithFirstLevel() {
        AdminCategoryQueryDTO queryDTO = AdminCategoryQueryDTO.builder()
                .level(1)
                .parentId(PARENT_CATEGORY_ID)
                .build();

        assertResultCode(ResultCode.PARAM_INVALID, () -> categoryService.getCategoryList(queryDTO));

        verifyNoInteractions(categoryMapper, articleMapper);
    }

    @Test
    void createCategoryShouldCreateSecondLevelUnderFirstLevelParent() {
        CategoryUpsertRequestDTO requestDTO = upsertRequest(
                PARENT_CATEGORY_ID,
                2,
                "Java"
        );
        when(categoryMapper.selectById(PARENT_CATEGORY_ID))
                .thenReturn(parentCategory(PARENT_CATEGORY_ID, "Tech", 10));
        when(categoryMapper.exists(any())).thenReturn(false);
        doAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId(FIRST_CHILD_CATEGORY_ID);
            return 1;
        }).when(categoryMapper).insert(any(Category.class));

        CreatedCategoryVO result = categoryService.createCategory(requestDTO);

        assertEquals(FIRST_CHILD_CATEGORY_ID, result.getId());
        assertEquals(PARENT_CATEGORY_ID, result.getParentId());
        assertEquals(2, result.getLevel());
        assertEquals("Java", result.getName());
        assertEquals(CategoryStatus.ENABLED, result.getStatus());
    }

    @Test
    void createCategoryShouldRejectParentForFirstLevel() {
        CategoryUpsertRequestDTO requestDTO = upsertRequest(PARENT_CATEGORY_ID, 1, "Tech");

        assertResultCode(
                ResultCode.CATEGORY_PARENT_NOT_ALLOWED,
                () -> categoryService.createCategory(requestDTO)
        );

        verify(categoryMapper, never()).insert(any(Category.class));
    }

    @Test
    void createCategoryShouldRequireParentForSecondLevel() {
        CategoryUpsertRequestDTO requestDTO = upsertRequest(null, 2, "Java");

        assertResultCode(
                ResultCode.CATEGORY_PARENT_REQUIRED,
                () -> categoryService.createCategory(requestDTO)
        );

        verify(categoryMapper, never()).insert(any(Category.class));
    }

    @Test
    void createCategoryShouldRejectMissingOrSecondLevelParent() {
        CategoryUpsertRequestDTO requestDTO = upsertRequest(PARENT_CATEGORY_ID, 2, "Java");
        when(categoryMapper.selectById(PARENT_CATEGORY_ID)).thenReturn(null);

        assertResultCode(
                ResultCode.CATEGORY_PARENT_INVALID,
                () -> categoryService.createCategory(requestDTO)
        );

        verify(categoryMapper, never()).insert(any(Category.class));
    }

    @Test
    void createCategoryShouldRejectDuplicateNameWithinSameParent() {
        CategoryUpsertRequestDTO requestDTO = upsertRequest(PARENT_CATEGORY_ID, 2, " java ");
        when(categoryMapper.selectById(PARENT_CATEGORY_ID))
                .thenReturn(parentCategory(PARENT_CATEGORY_ID, "Tech", 10));
        when(categoryMapper.exists(any())).thenReturn(true);

        assertResultCode(
                ResultCode.CATEGORY_NAME_ALREADY_EXISTS,
                () -> categoryService.createCategory(requestDTO)
        );

        verify(categoryMapper, never()).insert(any(Category.class));
    }

    @Test
    void updateCategoryShouldRejectMissingCategory() {
        when(categoryMapper.selectById(FIRST_CHILD_CATEGORY_ID)).thenReturn(null);

        assertResultCode(
                ResultCode.CATEGORY_NOT_FOUND,
                () -> categoryService.updateCategory(
                        FIRST_CHILD_CATEGORY_ID,
                        upsertRequest(PARENT_CATEGORY_ID, 2, "Java")
                )
        );

        verify(categoryMapper, never()).updateById(any(Category.class));
    }

    @Test
    void updateCategoryShouldRejectLevelChange() {
        when(categoryMapper.selectById(FIRST_CHILD_CATEGORY_ID)).thenReturn(
                childCategory(FIRST_CHILD_CATEGORY_ID, PARENT_CATEGORY_ID, "Java", 11)
        );

        assertResultCode(
                ResultCode.CATEGORY_UPDATE_LEVEL_NOT_ALLOWED,
                () -> categoryService.updateCategory(
                        FIRST_CHILD_CATEGORY_ID,
                        upsertRequest(null, 1, "Java")
                )
        );

        verify(categoryMapper, never()).updateById(any(Category.class));
    }

    @Test
    void updateCategoryShouldValidateAndReturnPersistedCategory() {
        Category current = childCategory(FIRST_CHILD_CATEGORY_ID, PARENT_CATEGORY_ID, "Java", 11);
        Category updated = childCategory(FIRST_CHILD_CATEGORY_ID, PARENT_CATEGORY_ID, "Java SE", 15);
        when(categoryMapper.selectById(FIRST_CHILD_CATEGORY_ID))
                .thenReturn(current)
                .thenReturn(updated);
        when(categoryMapper.selectById(PARENT_CATEGORY_ID))
                .thenReturn(parentCategory(PARENT_CATEGORY_ID, "Tech", 10));
        when(categoryMapper.exists(any())).thenReturn(false);

        UpdatedCategoryVO result = categoryService.updateCategory(
                FIRST_CHILD_CATEGORY_ID,
                upsertRequest(PARENT_CATEGORY_ID, 2, "Java SE")
        );

        assertEquals(FIRST_CHILD_CATEGORY_ID, result.getId());
        assertEquals(PARENT_CATEGORY_ID, result.getParentId());
        assertEquals("Java SE", result.getName());
        assertEquals(CategoryStatus.ENABLED, result.getStatus());
        verify(categoryMapper).updateById(any(Category.class));
    }

    @Test
    void deleteCategoryShouldRejectMissingCategory() {
        when(categoryMapper.selectById(PARENT_CATEGORY_ID)).thenReturn(null);

        assertResultCode(
                ResultCode.CATEGORY_NOT_FOUND,
                () -> categoryService.deleteCategory(PARENT_CATEGORY_ID)
        );

        verify(categoryMapper, never()).deleteById(anyLong());
    }

    @Test
    void deleteCategoryShouldRejectFirstLevelWithChildren() {
        when(categoryMapper.selectById(PARENT_CATEGORY_ID))
                .thenReturn(parentCategory(PARENT_CATEGORY_ID, "Tech", 10));
        when(categoryMapper.exists(any())).thenReturn(true);

        assertResultCode(
                ResultCode.CATEGORY_HAS_CHILDREN,
                () -> categoryService.deleteCategory(PARENT_CATEGORY_ID)
        );

        verify(categoryMapper, never()).deleteById(anyLong());
        verifyNoInteractions(articleMapper);
    }

    @Test
    void deleteCategoryShouldDeleteEmptyFirstLevel() {
        when(categoryMapper.selectById(PARENT_CATEGORY_ID))
                .thenReturn(parentCategory(PARENT_CATEGORY_ID, "Tech", 10));
        when(categoryMapper.exists(any())).thenReturn(false);

        categoryService.deleteCategory(PARENT_CATEGORY_ID);

        verify(categoryMapper).deleteById(PARENT_CATEGORY_ID);
        verifyNoInteractions(articleMapper);
    }

    @Test
    void deleteCategoryShouldRejectSecondLevelWithArticles() {
        when(categoryMapper.selectById(FIRST_CHILD_CATEGORY_ID)).thenReturn(
                childCategory(FIRST_CHILD_CATEGORY_ID, PARENT_CATEGORY_ID, "Java", 11)
        );
        when(articleMapper.exists(any())).thenReturn(true);

        assertResultCode(
                ResultCode.CATEGORY_HAS_ARTICLES,
                () -> categoryService.deleteCategory(FIRST_CHILD_CATEGORY_ID)
        );

        verify(categoryMapper, never()).deleteById(anyLong());
    }

    @Test
    void deleteCategoryShouldDeleteSecondLevelWithoutArticles() {
        when(categoryMapper.selectById(FIRST_CHILD_CATEGORY_ID)).thenReturn(
                childCategory(FIRST_CHILD_CATEGORY_ID, PARENT_CATEGORY_ID, "Java", 11)
        );
        when(articleMapper.exists(any())).thenReturn(false);

        categoryService.deleteCategory(FIRST_CHILD_CATEGORY_ID);

        verify(categoryMapper).deleteById(FIRST_CHILD_CATEGORY_ID);
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

    private CategoryUpsertRequestDTO upsertRequest(Long parentId, int level, String name) {
        return CategoryUpsertRequestDTO.builder()
                .parentId(parentId)
                .level(level)
                .name(name)
                .description(name + " description")
                .sortNo(10)
                .status(CategoryStatus.ENABLED)
                .build();
    }

    private void assertResultCode(ResultCode expected, Executable executable) {
        BizException exception = org.junit.jupiter.api.Assertions.assertThrows(
                BizException.class,
                executable
        );
        assertEquals(expected, exception.getResultCode());
    }
}
