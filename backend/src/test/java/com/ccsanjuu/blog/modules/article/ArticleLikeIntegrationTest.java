package com.ccsanjuu.blog.modules.article;

import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.common.util.TokenHashUtil;
import com.ccsanjuu.blog.modules.article.model.bo.ArticleLikeIdentity;
import com.ccsanjuu.blog.modules.article.service.ArticleLikeService;
import com.ccsanjuu.blog.modules.article.service.ArticleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ArticleLikeIntegrationTest {

    @Autowired
    private ArticleLikeService articleLikeService;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private Long articleId;
    private String visitorToken;
    private String rateLimitKey;

    @AfterEach
    void clearRateLimitKey() {
        if (rateLimitKey != null) {
            stringRedisTemplate.delete(rateLimitKey);
        }
    }

    @BeforeEach
    void setUp() {
        articleId = jdbcTemplate.queryForObject(
                "select id from blog_article where status = 'PUBLISHED' order by id limit 1",
                Long.class
        );
        visitorToken = UUID.randomUUID().toString();
        jdbcTemplate.update("delete from blog_article_like where article_id = ?", articleId);
        jdbcTemplate.update("update blog_article set like_count = 0 where id = ?", articleId);
    }

    @Test
    void visitorLikeShouldBeIdempotentAndCancelable() {
        ArticleLikeIdentity identity = new ArticleLikeIdentity(null, visitorToken);

        var liked = articleLikeService.likeArticle(articleId, identity);
        var repeated = articleLikeService.likeArticle(articleId, identity);

        assertTrue(liked.getLiked());
        assertEquals(1, liked.getLikeCount());
        assertEquals(1, repeated.getLikeCount());
        assertTrue(articleLikeService.isLiked(articleId, identity));

        var unliked = articleLikeService.unlikeArticle(articleId, identity);

        assertFalse(unliked.getLiked());
        assertEquals(0, unliked.getLikeCount());
        assertFalse(articleLikeService.isLiked(articleId, identity));
    }

    @Test
    void visitorLikeAndUnlikeShouldShareTheActorRateLimit() {
        ArticleLikeIdentity identity = new ArticleLikeIdentity(null, visitorToken);
        rateLimitKey = "blog:like:rate:actor:" + TokenHashUtil.sha256(
                "visitor:" + TokenHashUtil.sha256(visitorToken)
        );
        stringRedisTemplate.delete(rateLimitKey);

        for (int index = 0; index < 15; index++) {
            articleLikeService.likeArticle(articleId, identity);
            articleLikeService.unlikeArticle(articleId, identity);
        }

        BizException exception = assertThrows(BizException.class,
                () -> articleLikeService.likeArticle(articleId, identity));

        assertEquals(ResultCode.ARTICLE_LIKE_RATE_LIMITED, exception.getResultCode());
    }
}
