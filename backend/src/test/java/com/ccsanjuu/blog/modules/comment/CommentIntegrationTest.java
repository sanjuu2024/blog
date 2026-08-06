package com.ccsanjuu.blog.modules.comment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.article.mapper.ArticleMapper;
import com.ccsanjuu.blog.modules.article.model.entity.Article;
import com.ccsanjuu.blog.modules.article.model.enums.ArticleStatus;
import com.ccsanjuu.blog.modules.comment.mapper.CommentMapper;
import com.ccsanjuu.blog.modules.comment.model.bo.CommentReplyCountBO;
import com.ccsanjuu.blog.modules.comment.model.dto.CommentModerationRequestDTO;
import com.ccsanjuu.blog.modules.comment.model.dto.CommentReplyQueryDTO;
import com.ccsanjuu.blog.modules.comment.model.dto.CreateCommentRequestDTO;
import com.ccsanjuu.blog.modules.comment.model.dto.PublicCommentQueryDTO;
import com.ccsanjuu.blog.modules.comment.model.entity.Comment;
import com.ccsanjuu.blog.modules.comment.model.enums.CommentModerationAction;
import com.ccsanjuu.blog.modules.comment.model.enums.CommentStatus;
import com.ccsanjuu.blog.modules.comment.model.vo.CommentDeleteVO;
import com.ccsanjuu.blog.modules.comment.model.vo.CommentMutationVO;
import com.ccsanjuu.blog.modules.comment.model.vo.CommentReplyPageVO;
import com.ccsanjuu.blog.modules.comment.service.CommentService;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import com.ccsanjuu.blog.modules.user.model.enums.UserRole;
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
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void replyCountsShouldSeparateApprovedAndVisibleReplies() {
        Article article = findPublishedArticle();
        User user = findActiveUser();

        Comment firstRoot = insertComment(article.getId(), user.getId(), null, null, CommentStatus.APPROVED);
        Comment secondRoot = insertComment(article.getId(), user.getId(), null, null, CommentStatus.APPROVED);
        insertComment(article.getId(), user.getId(), firstRoot.getId(), firstRoot.getId(), CommentStatus.APPROVED);
        insertComment(article.getId(), user.getId(), firstRoot.getId(), firstRoot.getId(), CommentStatus.PENDING);

        List<CommentReplyCountBO> counts = commentMapper.selectReplyCounts(
                List.of(firstRoot.getId(), secondRoot.getId()),
                user.getId()
        );

        CommentReplyCountBO firstRootCount = counts.stream()
                .filter(item -> item.getRootId().equals(firstRoot.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(1L, firstRootCount.getReplyCount());
        assertEquals(2L, firstRootCount.getVisibleReplyCount());
        assertEquals(1, counts.size());
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

    @Test
    void deleteOwnCommentShouldReturnRealDeletedApprovedCount() {
        Article article = findPublishedArticle();
        User user = findActiveUser();

        article.setCommentCount(3);
        articleMapper.updateById(article);

        Comment root = insertComment(article.getId(), user.getId(), null, null, CommentStatus.APPROVED);
        Comment reply = insertComment(article.getId(), user.getId(), root.getId(), root.getId(), CommentStatus.APPROVED);
        Comment nestedReply = insertComment(article.getId(), user.getId(), reply.getId(), root.getId(), CommentStatus.PENDING);

        CommentDeleteVO result = commentService.deleteOwnComment(root.getId(), user.getId());
        Article updatedArticle = articleMapper.selectById(article.getId());

        assertEquals(2L, result.getDeletedApprovedCount());
        assertEquals(1, updatedArticle.getCommentCount());
        assertEquals(CommentStatus.DELETED, commentMapper.selectById(root.getId()).getStatus());
        assertEquals(CommentStatus.DELETED, commentMapper.selectById(reply.getId()).getStatus());
        assertEquals(CommentStatus.DELETED, commentMapper.selectById(nestedReply.getId()).getStatus());
    }

    @Test
    void moderationShouldKeepApprovedCommentCountConsistentAcrossTransitions() {
        Article article = findPublishedArticle();
        User user = findActiveUser();
        User admin = findAdminUser();
        int originalCount = article.getCommentCount();
        Comment comment = insertComment(article.getId(), user.getId(), null, null, CommentStatus.PENDING);

        commentService.moderateComment(comment.getId(), admin.getId(), CommentModerationRequestDTO.builder()
                .action(CommentModerationAction.APPROVE)
                .build());
        assertEquals(originalCount + 1, articleMapper.selectById(article.getId()).getCommentCount());

        commentService.moderateComment(comment.getId(), admin.getId(), CommentModerationRequestDTO.builder()
                .action(CommentModerationAction.HIDE)
                .reason("暂时隐藏")
                .build());
        assertEquals(originalCount, articleMapper.selectById(article.getId()).getCommentCount());

        commentService.moderateComment(comment.getId(), admin.getId(), CommentModerationRequestDTO.builder()
                .action(CommentModerationAction.APPROVE)
                .build());
        assertEquals(originalCount + 1, articleMapper.selectById(article.getId()).getCommentCount());
    }

    @Test
    void publicCommentListShouldOnlyExposeApprovedToVisitorsAndOwnPendingOrRejectedToAuthor() {
        Article article = findPublishedArticle();
        User user = findActiveUser();
        Comment approved = insertComment(article.getId(), user.getId(), null, null, CommentStatus.APPROVED);
        Comment pending = insertComment(article.getId(), user.getId(), null, null, CommentStatus.PENDING);
        Comment rejected = insertComment(article.getId(), user.getId(), null, null, CommentStatus.REJECTED);
        Comment hidden = insertComment(article.getId(), user.getId(), null, null, CommentStatus.HIDDEN);

        PublicCommentQueryDTO query = new PublicCommentQueryDTO();
        query.setPageSize(20);
        Set<Long> visitorIds = commentService.getPublicCommentList(article.getId(), null, query)
                .getRecords().stream().map(item -> item.getId()).collect(Collectors.toSet());
        Set<Long> authorIds = commentService.getPublicCommentList(article.getId(), user.getId(), query)
                .getRecords().stream().map(item -> item.getId()).collect(Collectors.toSet());

        assertTrue(visitorIds.contains(approved.getId()));
        assertFalse(visitorIds.contains(pending.getId()));
        assertFalse(visitorIds.contains(rejected.getId()));
        assertFalse(visitorIds.contains(hidden.getId()));
        assertTrue(authorIds.contains(pending.getId()));
        assertTrue(authorIds.contains(rejected.getId()));
        assertFalse(authorIds.contains(hidden.getId()));
    }

    @Test
    void repliesShouldUseCursorWithoutDuplicatesOrMissingRecords() {
        Article article = findPublishedArticle();
        User user = findActiveUser();
        Comment root = insertComment(article.getId(), user.getId(), null, null, CommentStatus.APPROVED);
        Comment first = insertComment(article.getId(), user.getId(), root.getId(), root.getId(), CommentStatus.APPROVED);
        Comment second = insertComment(article.getId(), user.getId(), root.getId(), root.getId(), CommentStatus.APPROVED);
        Comment third = insertComment(article.getId(), user.getId(), second.getId(), root.getId(), CommentStatus.APPROVED);

        CommentReplyQueryDTO firstQuery = new CommentReplyQueryDTO();
        firstQuery.setLimit(2);
        CommentReplyPageVO firstPage = commentService.getRepliesByRootId(root.getId(), null, firstQuery);
        assertEquals(2, firstPage.getRecords().size());
        assertTrue(firstPage.isHasNext());

        CommentReplyQueryDTO secondQuery = new CommentReplyQueryDTO();
        secondQuery.setLimit(2);
        secondQuery.setCursor(firstPage.getNextCursor());
        CommentReplyPageVO secondPage = commentService.getRepliesByRootId(root.getId(), null, secondQuery);
        Set<Long> ids = new HashSet<>();
        firstPage.getRecords().forEach(item -> ids.add(item.getId()));
        secondPage.getRecords().forEach(item -> ids.add(item.getId()));

        assertEquals(Set.of(first.getId(), second.getId(), third.getId()), ids);
        assertFalse(secondPage.isHasNext());
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

    private User findAdminUser() {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getRole, UserRole.ADMIN)
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
