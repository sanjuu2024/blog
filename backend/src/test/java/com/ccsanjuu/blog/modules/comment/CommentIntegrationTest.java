package com.ccsanjuu.blog.modules.comment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.article.mapper.ArticleMapper;
import com.ccsanjuu.blog.modules.article.model.entity.Article;
import com.ccsanjuu.blog.modules.article.model.enums.ArticleStatus;
import com.ccsanjuu.blog.modules.comment.mapper.CommentMapper;
import com.ccsanjuu.blog.modules.comment.model.dto.CreateCommentRequestDTO;
import com.ccsanjuu.blog.modules.comment.model.entity.Comment;
import com.ccsanjuu.blog.modules.comment.model.enums.CommentStatus;
import com.ccsanjuu.blog.modules.comment.model.vo.CommentMutationVO;
import com.ccsanjuu.blog.modules.comment.service.CommentService;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import com.ccsanjuu.blog.modules.user.model.enums.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class CommentIntegrationTest {

    private static final String ARTICLE_SLUG = "spring-boot-dual-token-login";
    private static final String USERNAME = "demo_alice";

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private CommentService commentService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private String rateLimitKey;

    @AfterEach
    void clearRateLimitKey() {
        if (rateLimitKey != null) {
            stringRedisTemplate.delete(rateLimitKey);
        }
    }

    @Test
    void recursiveCteShouldFindEntireCommentSubtree() {
        Article article = findPublishedArticle();
        User user = findActiveUser();

        Comment root = insertComment(article.getId(), user.getId(), null, null, CommentStatus.APPROVED);
        Comment reply = insertComment(article.getId(), user.getId(), root.getId(), root.getId(), CommentStatus.APPROVED);
        Comment nestedReply = insertComment(article.getId(), user.getId(), reply.getId(), root.getId(), CommentStatus.PENDING);

        List<Long> subtreeIds = commentMapper.selectCommentSubtreeIds(root.getId());

        assertEquals(Set.of(root.getId(), reply.getId(), nestedReply.getId()), new HashSet<>(subtreeIds));
        assertEquals(2L, commentMapper.countApprovedCommentSubtree(root.getId()));
    }

    @Test
    void commentCreationShouldUseRealRedisRateLimit() {
        Article article = findPublishedArticle();
        User user = findActiveUser();
        rateLimitKey = "blog:comment:rate:user:" + user.getId() + ":article:" + article.getId();
        stringRedisTemplate.delete(rateLimitKey);

        CommentMutationVO firstComment = commentService.createComment(
                article.getId(),
                user.getId(),
                CreateCommentRequestDTO.builder()
                        .content("集成测试评论")
                        .build()
        );

        assertNotNull(firstComment.getId());
        BizException exception = assertThrows(
                BizException.class,
                () -> commentService.createComment(
                        article.getId(),
                        user.getId(),
                        CreateCommentRequestDTO.builder()
                                .content("第二条集成测试评论")
                                .build()
                )
        );

        assertEquals(ResultCode.COMMENT_RATE_LIMITED, exception.getResultCode());
    }

    private Article findPublishedArticle() {
        Article article = articleMapper.selectOne(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getSlug, ARTICLE_SLUG)
                        .eq(Article::getStatus, ArticleStatus.PUBLISHED)
        );
        assertNotNull(article);
        return article;
    }

    private User findActiveUser() {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, USERNAME)
                        .eq(User::getStatus, UserStatus.ACTIVE)
        );
        assertNotNull(user);
        return user;
    }

    private Comment insertComment(
            Long articleId,
            Long userId,
            Long parentId,
            Long rootId,
            CommentStatus status
    ) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        Comment comment = Comment.builder()
                .articleId(articleId)
                .userId(userId)
                .parentId(parentId)
                .rootId(rootId)
                .content("评论集成测试数据")
                .status(status)
                .createdAt(now)
                .updatedAt(now)
                .build();
        commentMapper.insert(comment);
        return comment;
    }
}
