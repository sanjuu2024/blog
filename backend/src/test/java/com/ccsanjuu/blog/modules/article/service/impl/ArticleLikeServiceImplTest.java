package com.ccsanjuu.blog.modules.article.service.impl;

import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.article.mapper.ArticleLikeMapper;
import com.ccsanjuu.blog.modules.article.mapper.ArticleMapper;
import com.ccsanjuu.blog.modules.article.model.bo.ArticleLikeIdentity;
import com.ccsanjuu.blog.modules.article.model.entity.Article;
import com.ccsanjuu.blog.modules.article.model.entity.ArticleLike;
import com.ccsanjuu.blog.modules.category.mapper.CategoryMapper;
import com.ccsanjuu.blog.modules.category.model.entity.Category;
import com.ccsanjuu.blog.modules.category.model.enums.CategoryStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArticleLikeServiceImplTest {

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private ArticleLikeMapper articleLikeMapper;

    @Mock
    private CategoryMapper categoryMapper;

    private ArticleLikeServiceImpl articleLikeService;

    @BeforeEach
    void setUp() {
        articleLikeService = new ArticleLikeServiceImpl(articleMapper, articleLikeMapper, categoryMapper);
    }

    @Test
    void likeArticleShouldInsertAndIncrementCount() {
        stubPublicCategory();
        when(articleMapper.selectById(40001L)).thenReturn(article(3), article(4));
        when(articleLikeMapper.selectByActor(eq(40001L), eq(10001L), eq(null))).thenReturn(null);
        when(articleLikeMapper.insertIgnore(any(ArticleLike.class))).thenReturn(1);
        when(articleMapper.incrementLikeCount(40001L)).thenReturn(1);

        var result = articleLikeService.likeArticle(40001L, new ArticleLikeIdentity(10001L, null));

        assertTrue(result.getLiked());
        assertEquals(4, result.getLikeCount());
        ArgumentCaptor<ArticleLike> captor = ArgumentCaptor.forClass(ArticleLike.class);
        verify(articleLikeMapper).insertIgnore(captor.capture());
        assertEquals(10001L, captor.getValue().getUserId());
        assertEquals(null, captor.getValue().getVisitorTokenHash());
    }

    @Test
    void repeatedLikeShouldBeIdempotent() {
        stubPublicCategory();
        when(articleMapper.selectById(40001L)).thenReturn(article(3), article(3));
        when(articleLikeMapper.selectByActor(eq(40001L), eq(10001L), eq(null)))
                .thenReturn(ArticleLike.builder().articleId(40001L).userId(10001L).build());

        var result = articleLikeService.likeArticle(40001L, new ArticleLikeIdentity(10001L, null));

        assertTrue(result.getLiked());
        assertEquals(3, result.getLikeCount());
        verify(articleLikeMapper, never()).insertIgnore(any(ArticleLike.class));
        verify(articleMapper, never()).incrementLikeCount(40001L);
    }

    @Test
    void visitorLikeShouldStoreHashedIdentity() {
        stubPublicCategory();
        when(articleMapper.selectById(40001L)).thenReturn(article(0), article(1));
        when(articleLikeMapper.selectByActor(eq(40001L), eq(null), any(String.class))).thenReturn(null);
        when(articleLikeMapper.insertIgnore(any(ArticleLike.class))).thenReturn(1);

        articleLikeService.likeArticle(40001L, new ArticleLikeIdentity(null, "visitor-token"));

        ArgumentCaptor<ArticleLike> captor = ArgumentCaptor.forClass(ArticleLike.class);
        verify(articleLikeMapper).insertIgnore(captor.capture());
        assertEquals(null, captor.getValue().getUserId());
        assertTrue(captor.getValue().getVisitorTokenHash().startsWith("sha256:"));
    }

    @Test
    void unlikeArticleShouldDeleteAndDecrementCount() {
        when(articleMapper.selectById(40001L)).thenReturn(article(3), article(2));
        when(articleLikeMapper.deleteByActor(40001L, 10001L, null)).thenReturn(1);
        when(articleMapper.decrementLikeCount(40001L)).thenReturn(1);

        var result = articleLikeService.unlikeArticle(40001L, new ArticleLikeIdentity(10001L, null));

        assertFalse(result.getLiked());
        assertEquals(2, result.getLikeCount());
        verify(articleMapper).decrementLikeCount(40001L);
    }

    @Test
    void likeArticleShouldRejectNonPublishedArticle() {
        when(articleMapper.selectById(40001L)).thenReturn(article(3, "OFFLINE"));

        BizException exception = assertThrows(
                BizException.class,
                () -> articleLikeService.likeArticle(40001L, new ArticleLikeIdentity(10001L, null))
        );

        assertEquals(ResultCode.ARTICLE_NOT_VISIBLE, exception.getResultCode());
        verify(articleLikeMapper, never()).insertIgnore(any(ArticleLike.class));
    }

    private Article article(int likeCount) {
        return article(likeCount, "PUBLISHED");
    }

    private Article article(int likeCount, String status) {
        return Article.builder()
                .id(40001L)
                .categoryId(21001L)
                .status(com.ccsanjuu.blog.modules.article.model.enums.ArticleStatus.valueOf(status))
                .likeCount(likeCount)
                .build();
    }

    private Category category(Long id, Long parentId) {
        return Category.builder()
                .id(id)
                .parentId(parentId)
                .level(parentId == null ? 1 : 2)
                .status(CategoryStatus.ENABLED)
                .build();
    }

    private void stubPublicCategory() {
        when(categoryMapper.selectById(21001L)).thenReturn(category(21001L, 20001L));
        when(categoryMapper.selectById(20001L)).thenReturn(category(20001L, null));
    }
}
