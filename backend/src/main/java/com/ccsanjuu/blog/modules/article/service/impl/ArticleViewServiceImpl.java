package com.ccsanjuu.blog.modules.article.service.impl;

import com.ccsanjuu.blog.modules.article.mapper.ArticleDailyStatMapper;
import com.ccsanjuu.blog.modules.article.mapper.ArticleMapper;
import com.ccsanjuu.blog.modules.article.model.bo.ArticleViewIdentity;
import com.ccsanjuu.blog.modules.article.service.ArticleViewService;
import com.ccsanjuu.blog.common.util.TokenHashUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleViewServiceImpl implements ArticleViewService {

    private static final long DEDUPLICATION_SECONDS = 60 * 60;
    private static final String VIEW_KEY_PREFIX = "blog:article:view:";
    // 每日趋势统一按上海时区自然日归档。
    private static final ZoneId STAT_ZONE = ZoneId.of("Asia/Shanghai");
    // SET NX 保证并发请求只有一个主体能取得该文章的一小时计数权。（同一个用户/游客多个请求只需要有一个即可，如果这些请求并发，不需要等待，只要有一个生效了即可）
    private static final DefaultRedisScript<Long> ACQUIRE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('set', KEYS[1], ARGV[1], 'EX', ARGV[2], 'NX') then return 1 else return 0 end",
            Long.class
    );
    private static final DefaultRedisScript<Long> RELEASE_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    private final ArticleMapper articleMapper;
    private final ArticleDailyStatMapper articleDailyStatMapper;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 去重 key 与数据库计数一起提交；事务回滚时释放 key，避免失败请求占用一小时计数额度。
     *
     * @param articleId 文章 ID
     * @param identity 登录用户或游客身份
     * @return 当前文章有效浏览总数
     */
    @Override
    @Transactional
    public Integer recordView(Long articleId, ArticleViewIdentity identity) {
        if (identity == null || (!identity.isAuthenticated() && !StringUtils.hasText(identity.visitorToken()))) {
            return currentViewCount(articleId);
        }

        String marker = UUID.randomUUID().toString();
        String deduplicationKey = buildDeduplicationKey(articleId, identity);
        if (!tryAcquire(deduplicationKey, marker)) {
            return currentViewCount(articleId);
        }

        try {
            if (articleMapper.incrementViewCount(articleId) == 0) {
                release(deduplicationKey, marker);
                return currentViewCount(articleId);
            }
            articleDailyStatMapper.incrementViewCount(articleId, LocalDate.now(STAT_ZONE));
            registerRollbackRelease(deduplicationKey, marker);
            return currentViewCount(articleId);
        } catch (RuntimeException exception) {
            release(deduplicationKey, marker);
            throw exception;
        }
    }

    private Integer currentViewCount(Long articleId) {
        var article = articleMapper.selectById(articleId);
        return article == null || article.getViewCount() == null ? 0 : article.getViewCount();
    }

    private boolean tryAcquire(String key, String marker) {
        try {
            Long result = stringRedisTemplate.execute(
                    ACQUIRE_SCRIPT,
                    List.of(key),
                    marker,
                    String.valueOf(DEDUPLICATION_SECONDS)
            );
            return result != null && result == 1L;
        } catch (RuntimeException exception) {
            log.warn("Article view deduplication unavailable, skip counting: key={}", key, exception);
            return false;
        }
    }

    private void registerRollbackRelease(String key, String marker) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    release(key, marker);
                }
            }
        });
    }

    private void release(String key, String marker) {
        try {
            stringRedisTemplate.execute(RELEASE_SCRIPT, List.of(key), marker);
        } catch (RuntimeException exception) {
            log.warn("Failed to release article view deduplication key: key={}", key, exception);
        }
    }

    private String buildDeduplicationKey(Long articleId, ArticleViewIdentity identity) {
        String subject = identity.isAuthenticated()
                ? "user:" + identity.userId()
                : "visitor:" + identity.visitorToken();
        return VIEW_KEY_PREFIX + articleId + ":" + TokenHashUtil.sha256(subject);
    }
}
