package com.ccsanjuu.blog.modules.article.service.impl;

import com.ccsanjuu.blog.modules.article.mapper.ArticleDailyStatMapper;
import com.ccsanjuu.blog.modules.article.mapper.ArticleMapper;
import com.ccsanjuu.blog.modules.article.model.bo.ArticleViewIdentity;
import com.ccsanjuu.blog.modules.article.model.entity.Article;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class ArticleViewServiceImplTest {

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private ArticleDailyStatMapper articleDailyStatMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void firstViewShouldIncrementArticleAndDailyCount() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(1L);
        when(articleMapper.incrementViewCount(40001L)).thenReturn(1);
        when(articleMapper.selectById(40001L)).thenReturn(articleWithViewCount(129));

        ArticleViewServiceImpl service = new ArticleViewServiceImpl(
                articleMapper,
                articleDailyStatMapper,
                stringRedisTemplate
        );

        Integer result = service.recordView(40001L, new ArticleViewIdentity(10001L, null));

        assertEquals(129, result);
        verify(articleDailyStatMapper).incrementViewCount(any(), any());
    }

    @Test
    void duplicateViewShouldOnlyReadCurrentCount() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenReturn(0L);
        when(articleMapper.selectById(40001L)).thenReturn(articleWithViewCount(128));

        ArticleViewServiceImpl service = new ArticleViewServiceImpl(
                articleMapper,
                articleDailyStatMapper,
                stringRedisTemplate
        );

        assertEquals(128, service.recordView(40001L, new ArticleViewIdentity(null, "visitor-token")));

        verify(articleMapper, never()).incrementViewCount(any());
        verify(articleDailyStatMapper, never()).incrementViewCount(any(), any());
    }

    @Test
    void redisFailureShouldLeaveDatabaseCountsUnchanged() {
        when(stringRedisTemplate.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenThrow(new IllegalStateException("redis unavailable"));
        when(articleMapper.selectById(40001L)).thenReturn(articleWithViewCount(128));

        ArticleViewServiceImpl service = new ArticleViewServiceImpl(
                articleMapper,
                articleDailyStatMapper,
                stringRedisTemplate
        );

        assertEquals(128, service.recordView(40001L, new ArticleViewIdentity(null, "visitor-token")));

        verify(articleMapper, never()).incrementViewCount(any());
        verify(articleDailyStatMapper, never()).incrementViewCount(any(), any());
    }

    private Article articleWithViewCount(int viewCount) {
        return Article.builder().id(40001L).viewCount(viewCount).build();
    }
}
