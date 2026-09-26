package com.ccsanjuu.blog.modules.article;

import com.ccsanjuu.blog.common.util.TokenHashUtil;
import com.ccsanjuu.blog.modules.article.model.bo.ArticleViewIdentity;
import com.ccsanjuu.blog.modules.article.model.vo.PublicArticleDetailVO;
import com.ccsanjuu.blog.modules.article.service.ArticleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ArticleViewIntegrationTest {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private Long articleId;
    private String visitorToken;
    private LocalDate statDate;

    @BeforeEach
    void setUp() {
        articleId = jdbcTemplate.queryForObject(
                "select id from blog_article where status = 'PUBLISHED' order by id limit 1",
                Long.class
        );
        jdbcTemplate.update(
                "delete from blog_article_daily_stat where article_id = ? and stat_date = ?",
                articleId,
                LocalDate.now(ZoneId.of("Asia/Shanghai"))
        );
        visitorToken = UUID.randomUUID().toString();
        statDate = LocalDate.now(ZoneId.of("Asia/Shanghai"));
    }

    @AfterEach
    void clearDeduplicationKey() {
        stringRedisTemplate.delete(viewKey());
    }

    @Test
    void detailReadShouldCountOnlyOncePerVisitorWithinOneHour() {
        int initialViewCount = jdbcTemplate.queryForObject(
                "select view_count from blog_article where id = ?",
                Integer.class,
                articleId
        );
        Long initialDailyCount = jdbcTemplate.queryForObject(
                "select coalesce(sum(view_count), 0) from blog_article_daily_stat "
                        + "where article_id = ? and stat_date = ?",
                Long.class,
                articleId,
                statDate
        );

        PublicArticleDetailVO firstRead = articleService.getPublicArticleDetail(
                articleId,
                new ArticleViewIdentity(null, visitorToken)
        );
        PublicArticleDetailVO duplicateRead = articleService.getPublicArticleDetail(
                articleId,
                new ArticleViewIdentity(null, visitorToken)
        );

        assertEquals(initialViewCount + 1, firstRead.getViewCount());
        assertEquals(firstRead.getViewCount(), duplicateRead.getViewCount());
        assertEquals(initialDailyCount + 1, jdbcTemplate.queryForObject(
                "select view_count from blog_article_daily_stat "
                        + "where article_id = ? and stat_date = ?",
                Long.class,
                articleId,
                statDate
        ));
    }

    private String viewKey() {
        return "blog:article:view:" + articleId + ":"
                + TokenHashUtil.sha256("visitor:" + visitorToken);
    }
}
