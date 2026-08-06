package com.ccsanjuu.blog.modules.comment.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.article.mapper.ArticleMapper;
import com.ccsanjuu.blog.modules.article.model.entity.Article;
import com.ccsanjuu.blog.modules.article.model.enums.ArticleStatus;
import com.ccsanjuu.blog.modules.comment.mapper.CommentMapper;
import com.ccsanjuu.blog.modules.comment.model.bo.CommentReplyCountBO;
import com.ccsanjuu.blog.modules.comment.model.dto.AdminCommentQueryDTO;
import com.ccsanjuu.blog.modules.comment.model.dto.CommentModerationRequestDTO;
import com.ccsanjuu.blog.modules.comment.model.dto.CommentReplyQueryDTO;
import com.ccsanjuu.blog.modules.comment.model.dto.CreateCommentRequestDTO;
import com.ccsanjuu.blog.modules.comment.model.dto.PublicCommentQueryDTO;
import com.ccsanjuu.blog.modules.comment.model.entity.Comment;
import com.ccsanjuu.blog.modules.comment.model.enums.CommentModerationAction;
import com.ccsanjuu.blog.modules.comment.model.enums.CommentStatus;
import com.ccsanjuu.blog.modules.comment.model.enums.CommentType;
import com.ccsanjuu.blog.modules.comment.model.vo.AdminCommentItemVO;
import com.ccsanjuu.blog.modules.comment.model.vo.CommentDeleteVO;
import com.ccsanjuu.blog.modules.comment.model.vo.CommentMutationVO;
import com.ccsanjuu.blog.modules.comment.model.vo.PublicCommentItemVO;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import com.ccsanjuu.blog.modules.user.model.enums.UserRole;
import com.ccsanjuu.blog.modules.user.model.enums.UserStatus;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"rawtypes", "unchecked"})
class CommentServiceImplTest {

    private static final Long COMMENT_ID = 50001L;
    private static final Long ARTICLE_ID = 40001L;
    private static final Long USER_ID = 10001L;
    private static final Long ADMIN_ID = 10002L;
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-05-08T09:20:00+08:00");

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private CommentServiceImpl commentService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        initTableInfo(Comment.class);
    }

    @BeforeEach
    void setUp() {
        commentService = new CommentServiceImpl(articleMapper, commentMapper, userMapper, stringRedisTemplate);
        ReflectionTestUtils.setField(commentService, "baseMapper", commentMapper);
        ReflectionTestUtils.setField(commentService, "entityClass", Comment.class);
        ReflectionTestUtils.setField(commentService, "mapperClass", CommentMapper.class);
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.setIfAbsent(any(), any(), any(Duration.class))).thenReturn(true);
    }

    private static void initTableInfo(Class<?> entityClass) {
        if (com.baomidou.mybatisplus.core.metadata.TableInfoHelper.getTableInfo(entityClass) != null) {
            return;
        }
        MapperBuilderAssistant mapperBuilderAssistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        mapperBuilderAssistant.setCurrentNamespace(entityClass.getName());
        com.baomidou.mybatisplus.core.metadata.TableInfoHelper.initTableInfo(mapperBuilderAssistant, entityClass);
    }

    @Test
    void getAdminCommentListShouldFilterByTypeAndAttachRelations() {
        AdminCommentQueryDTO query = new AdminCommentQueryDTO();
        query.setPageNum(1);
        query.setPageSize(10);
        query.setArticleId(ARTICLE_ID);
        query.setUserId(USER_ID);
        query.setStatus(CommentStatus.APPROVED);
        query.setType(CommentType.TOP_LEVEL);

        when(articleMapper.selectById(ARTICLE_ID)).thenReturn(Article.builder().id(ARTICLE_ID).build());
        when(userMapper.selectById(USER_ID)).thenReturn(User.builder().id(USER_ID).username("alice").build());
        Comment comment = Comment.builder()
                .id(COMMENT_ID)
                .articleId(ARTICLE_ID)
                .userId(USER_ID)
                .content("这篇文章不错")
                .status(CommentStatus.APPROVED)
                .createdAt(CREATED_AT)
                .build();
        when(commentMapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
            Page<Comment> page = invocation.getArgument(0);
            page.setTotal(1);
            page.setRecords(List.of(comment));
            return page;
        });
        when(articleMapper.selectByIds(List.of(ARTICLE_ID))).thenReturn(List.of(Article.builder().id(ARTICLE_ID).build()));
        when(userMapper.selectByIds(List.of(USER_ID))).thenReturn(List.of(User.builder().id(USER_ID).username("alice").build()));

        PageResult<AdminCommentItemVO> result = commentService.getAdminCommentList(query);

        assertEquals(1, result.getTotal());
        assertEquals(1, result.getRecords().size());
        assertEquals(CommentType.TOP_LEVEL, result.getRecords().getFirst().getType());
        verify(commentMapper).selectPage(any(Page.class), any());
    }

    @Test
    void moderateCommentShouldConvertApproveActionToApprovedStatus() {
        Comment existingComment = Comment.builder()
                .id(COMMENT_ID)
                .articleId(ARTICLE_ID)
                .userId(USER_ID)
                .status(CommentStatus.PENDING)
                .content("这篇文章不错")
                .createdAt(CREATED_AT)
                .build();
        Comment updatedComment = Comment.builder()
                .id(COMMENT_ID)
                .articleId(ARTICLE_ID)
                .userId(USER_ID)
                .status(CommentStatus.APPROVED)
                .content("这篇文章不错")
                .reviewedBy(ADMIN_ID)
                .reviewedAt(CREATED_AT)
                .createdAt(CREATED_AT)
                .build();
        when(commentMapper.selectById(COMMENT_ID)).thenReturn(existingComment, updatedComment);
        when(commentMapper.selectByIdForUpdate(COMMENT_ID)).thenReturn(existingComment);
        when(commentMapper.update(any(Wrapper.class))).thenReturn(1);
        when(userMapper.selectById(USER_ID)).thenReturn(User.builder()
                .id(USER_ID)
                .username("alice")
                .nickname("Alice")
                .build());

        CommentModerationRequestDTO request = CommentModerationRequestDTO.builder()
                .action(CommentModerationAction.APPROVE)
                .build();

        CommentMutationVO result = commentService.moderateComment(COMMENT_ID, ADMIN_ID, request);

        assertEquals(CommentStatus.APPROVED, result.getStatus());
        assertEquals("alice", result.getAuthor().getUsername());
        verify(commentMapper).selectByIdForUpdate(COMMENT_ID);
        ArgumentCaptor<Wrapper<Comment>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(commentMapper).update(captor.capture());
        String sqlSet = ((LambdaUpdateWrapper<Comment>) captor.getValue()).getSqlSet();
        assertTrue(sqlSet.contains("status"));
        assertFalse(((LambdaUpdateWrapper<Comment>) captor.getValue())
                .getParamNameValuePairs()
                .containsValue(CommentModerationAction.APPROVE));
        verify(articleMapper).update(any(), any(Wrapper.class));
    }

    @Test
    void moderateCommentShouldRejectBlankReasonForDelete() {
        Comment comment = Comment.builder()
                .id(COMMENT_ID)
                .articleId(ARTICLE_ID)
                .userId(USER_ID)
                .status(CommentStatus.APPROVED)
                .content("这篇文章不错")
                .createdAt(CREATED_AT)
                .build();
        when(commentMapper.selectById(COMMENT_ID)).thenReturn(comment);
        when(commentMapper.selectByIdForUpdate(COMMENT_ID)).thenReturn(comment);

        CommentModerationRequestDTO request = CommentModerationRequestDTO.builder()
                .action(CommentModerationAction.DELETE)
                .reason("   ")
                .build();

        BizException exception = assertThrows(BizException.class,
                () -> commentService.moderateComment(COMMENT_ID, ADMIN_ID, request));

        assertEquals(ResultCode.COMMENT_MODERATION_REASON_REQUIRED, exception.getResultCode());
        verify(commentMapper, never()).update(any());
    }

    @Test
    void moderateCommentShouldRejectApproveWhenAlreadyApproved() {
        Comment comment = Comment.builder()
                .id(COMMENT_ID)
                .articleId(ARTICLE_ID)
                .userId(USER_ID)
                .status(CommentStatus.APPROVED)
                .content("这篇文章不错")
                .createdAt(CREATED_AT)
                .build();
        when(commentMapper.selectById(COMMENT_ID)).thenReturn(comment);
        when(commentMapper.selectByIdForUpdate(COMMENT_ID)).thenReturn(comment);

        CommentModerationRequestDTO request = CommentModerationRequestDTO.builder()
                .action(CommentModerationAction.APPROVE)
                .build();

        BizException exception = assertThrows(BizException.class,
                () -> commentService.moderateComment(COMMENT_ID, ADMIN_ID, request));

        assertEquals(ResultCode.COMMENT_STATUS_TRANSITION_INVALID, exception.getResultCode());
        verify(commentMapper, never()).update(any());
    }

    @Test
    void moderateCommentShouldWriteDeletedFieldsForDelete() {
        Comment existingComment = Comment.builder()
                .id(COMMENT_ID)
                .articleId(ARTICLE_ID)
                .userId(USER_ID)
                .status(CommentStatus.APPROVED)
                .content("这篇文章不错")
                .createdAt(CREATED_AT)
                .build();
        Comment updatedComment = Comment.builder()
                .id(COMMENT_ID)
                .articleId(ARTICLE_ID)
                .userId(USER_ID)
                .status(CommentStatus.DELETED)
                .content("这篇文章不错")
                .deletedBy(ADMIN_ID)
                .deletedAt(CREATED_AT)
                .moderationReason("删除测试")
                .createdAt(CREATED_AT)
                .build();
        when(commentMapper.selectById(COMMENT_ID)).thenReturn(existingComment, updatedComment);
        when(commentMapper.selectByIdForUpdate(COMMENT_ID)).thenReturn(existingComment);
        when(commentMapper.selectCommentSubtreeIds(COMMENT_ID)).thenReturn(List.of(COMMENT_ID, COMMENT_ID + 1, COMMENT_ID + 2));
        when(commentMapper.countApprovedCommentSubtree(COMMENT_ID)).thenReturn(2L);
        when(commentMapper.update(any(Wrapper.class))).thenReturn(1);
        when(userMapper.selectById(USER_ID)).thenReturn(User.builder()
                .id(USER_ID)
                .username("alice")
                .nickname("Alice")
                .build());

        CommentModerationRequestDTO request = CommentModerationRequestDTO.builder()
                .action(CommentModerationAction.DELETE)
                .reason("删除测试")
                .build();

        CommentMutationVO result = commentService.moderateComment(COMMENT_ID, ADMIN_ID, request);

        assertEquals(CommentStatus.DELETED, result.getStatus());
        assertEquals("alice", result.getAuthor().getUsername());
        verify(commentMapper).selectByIdForUpdate(COMMENT_ID);
        ArgumentCaptor<Wrapper<Comment>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(commentMapper, times(2)).update(captor.capture());
        String sqlSet = ((LambdaUpdateWrapper<Comment>) captor.getAllValues().getFirst()).getSqlSet();
        assertTrue(sqlSet.contains("deleted_by"));
        assertTrue(sqlSet.contains("deleted_at"));
        verify(articleMapper).update(any(), any(Wrapper.class));
    }

    @Test
    void getPublicCommentListShouldReturnMinePendingComment() {
        PublicCommentQueryDTO query = new PublicCommentQueryDTO();
        query.setPageNum(1);
        query.setPageSize(10);

        Comment comment = Comment.builder()
                .id(COMMENT_ID)
                .articleId(ARTICLE_ID)
                .userId(USER_ID)
                .content("等待审核的评论")
                .status(CommentStatus.PENDING)
                .createdAt(CREATED_AT)
                .build();
        when(articleMapper.selectById(ARTICLE_ID)).thenReturn(Article.builder()
                .id(ARTICLE_ID)
                .status(ArticleStatus.PUBLISHED)
                .build());
        when(commentMapper.selectPage(any(Page.class), any())).thenAnswer(invocation -> {
            Page<Comment> page = invocation.getArgument(0);
            page.setTotal(1);
            page.setRecords(List.of(comment));
            return page;
        });
        when(userMapper.selectByIds(List.of(USER_ID))).thenReturn(List.of(User.builder()
                .id(USER_ID)
                .username("alice")
                .nickname("Alice")
                .build()));
        when(commentMapper.selectReplyCounts(anyList(), eq(USER_ID))).thenReturn(List.of(
                new CommentReplyCountBO(COMMENT_ID, 3L, 4L)
        ));

        PageResult<PublicCommentItemVO> result = commentService.getPublicCommentList(ARTICLE_ID, USER_ID, query);

        assertEquals(1, result.getTotal());
        assertEquals(CommentStatus.PENDING, result.getRecords().getFirst().getStatus());
        assertTrue(result.getRecords().getFirst().getIsMine());
        assertEquals(3L, result.getRecords().getFirst().getReplyCount());
        assertTrue(result.getRecords().getFirst().getHasVisibleReplies());
        verify(commentMapper).selectReplyCounts(List.of(COMMENT_ID), USER_ID);
    }

    @Test
    void getRepliesShouldRejectWhenArticleIsNotPublished() {
        when(commentMapper.selectById(COMMENT_ID)).thenReturn(Comment.builder()
                .id(COMMENT_ID)
                .articleId(ARTICLE_ID)
                .userId(USER_ID)
                .status(CommentStatus.APPROVED)
                .build());
        when(articleMapper.selectById(ARTICLE_ID)).thenReturn(Article.builder()
                .id(ARTICLE_ID)
                .status(ArticleStatus.OFFLINE)
                .build());

        BizException exception = assertThrows(BizException.class,
                () -> commentService.getRepliesByRootId(COMMENT_ID, null, new CommentReplyQueryDTO()));

        assertEquals(ResultCode.COMMENT_NOT_FOUND, exception.getResultCode());
        verify(commentMapper, never()).selectPage(any(Page.class), any());
    }

    @Test
    void createCommentShouldCreatePendingCommentForNormalUser() {
        when(articleMapper.selectById(ARTICLE_ID)).thenReturn(Article.builder()
                .id(ARTICLE_ID)
                .status(ArticleStatus.PUBLISHED)
                .allowComment(true)
                .build());
        when(userMapper.selectById(USER_ID)).thenReturn(User.builder()
                .id(USER_ID)
                .username("alice")
                .nickname("Alice")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
        when(commentMapper.insert(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(COMMENT_ID);
            return 1;
        });
        when(commentMapper.selectById(COMMENT_ID)).thenReturn(Comment.builder()
                .id(COMMENT_ID)
                .articleId(ARTICLE_ID)
                .userId(USER_ID)
                .content("这篇文章不错")
                .status(CommentStatus.PENDING)
                .createdAt(CREATED_AT)
                .build());

        CommentMutationVO result = commentService.createComment(ARTICLE_ID, USER_ID, CreateCommentRequestDTO.builder()
                .content("  这篇文章不错  ")
                .build());

        assertEquals(CommentStatus.PENDING, result.getStatus());
        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentMapper).insert(captor.capture());
        assertEquals("这篇文章不错", captor.getValue().getContent());
        assertEquals(CommentStatus.PENDING, captor.getValue().getStatus());
        verify(articleMapper, never()).update(any(), any(Wrapper.class));
    }

    @Test
    void createCommentShouldCreateApprovedCommentForAdminAndIncreaseCount() {
        when(articleMapper.selectById(ARTICLE_ID)).thenReturn(Article.builder()
                .id(ARTICLE_ID)
                .status(ArticleStatus.PUBLISHED)
                .allowComment(true)
                .build());
        when(userMapper.selectById(ADMIN_ID)).thenReturn(User.builder()
                .id(ADMIN_ID)
                .username("admin")
                .nickname("Admin")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build());
        when(commentMapper.insert(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(COMMENT_ID);
            return 1;
        });
        when(commentMapper.selectById(COMMENT_ID)).thenReturn(Comment.builder()
                .id(COMMENT_ID)
                .articleId(ARTICLE_ID)
                .userId(ADMIN_ID)
                .content("管理员评论")
                .status(CommentStatus.APPROVED)
                .createdAt(CREATED_AT)
                .build());

        CommentMutationVO result = commentService.createComment(ARTICLE_ID, ADMIN_ID, CreateCommentRequestDTO.builder()
                .content("管理员评论")
                .build());

        assertEquals(CommentStatus.APPROVED, result.getStatus());
        verify(articleMapper).update(any(), any(Wrapper.class));
    }

    @Test
    void createReplyShouldLockRootCommentAndRecheckParent() {
        Long rootId = COMMENT_ID;
        Long parentId = COMMENT_ID + 1;
        Long newCommentId = COMMENT_ID + 2;
        Comment parent = Comment.builder()
                .id(parentId)
                .articleId(ARTICLE_ID)
                .userId(ADMIN_ID)
                .parentId(rootId)
                .rootId(rootId)
                .status(CommentStatus.APPROVED)
                .build();
        when(articleMapper.selectById(ARTICLE_ID)).thenReturn(Article.builder()
                .id(ARTICLE_ID)
                .status(ArticleStatus.PUBLISHED)
                .allowComment(true)
                .build());
        when(userMapper.selectById(USER_ID)).thenReturn(User.builder()
                .id(USER_ID)
                .username("alice")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
        when(commentMapper.selectById(parentId)).thenReturn(parent, parent);
        when(commentMapper.selectByIdForUpdate(rootId)).thenReturn(Comment.builder()
                .id(rootId)
                .articleId(ARTICLE_ID)
                .userId(ADMIN_ID)
                .status(CommentStatus.APPROVED)
                .build());
        when(commentMapper.insert(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(newCommentId);
            return 1;
        });
        when(commentMapper.selectById(newCommentId)).thenReturn(Comment.builder()
                .id(newCommentId)
                .articleId(ARTICLE_ID)
                .userId(USER_ID)
                .parentId(parentId)
                .rootId(rootId)
                .content("回复内容")
                .status(CommentStatus.PENDING)
                .createdAt(CREATED_AT)
                .build());

        CommentMutationVO result = commentService.createComment(ARTICLE_ID, USER_ID, CreateCommentRequestDTO.builder()
                .parentId(parentId)
                .content("回复内容")
                .build());

        assertEquals(rootId, result.getRootId());
        verify(commentMapper).selectByIdForUpdate(rootId);
        verify(commentMapper, times(2)).selectById(parentId);
    }

    @Test
    void createReplyShouldRejectParentHiddenWhileWaitingForTreeLock() {
        Long parentId = COMMENT_ID + 1;
        Comment initialParent = Comment.builder()
                .id(parentId)
                .articleId(ARTICLE_ID)
                .userId(ADMIN_ID)
                .parentId(COMMENT_ID)
                .rootId(COMMENT_ID)
                .status(CommentStatus.APPROVED)
                .build();
        Comment hiddenParent = Comment.builder()
                .id(parentId)
                .articleId(ARTICLE_ID)
                .userId(ADMIN_ID)
                .parentId(COMMENT_ID)
                .rootId(COMMENT_ID)
                .status(CommentStatus.HIDDEN)
                .build();
        when(articleMapper.selectById(ARTICLE_ID)).thenReturn(Article.builder()
                .id(ARTICLE_ID)
                .status(ArticleStatus.PUBLISHED)
                .allowComment(true)
                .build());
        when(userMapper.selectById(USER_ID)).thenReturn(User.builder()
                .id(USER_ID)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
        when(commentMapper.selectById(parentId)).thenReturn(initialParent, hiddenParent);
        when(commentMapper.selectByIdForUpdate(COMMENT_ID)).thenReturn(Comment.builder()
                .id(COMMENT_ID)
                .status(CommentStatus.APPROVED)
                .build());

        BizException exception = assertThrows(BizException.class,
                () -> commentService.createComment(ARTICLE_ID, USER_ID, CreateCommentRequestDTO.builder()
                        .parentId(parentId)
                        .content("回复内容")
                        .build()));

        assertEquals(ResultCode.COMMENT_PARENT_UNAVAILABLE, exception.getResultCode());
        verify(commentMapper, never()).insert(any(Comment.class));
    }

    @Test
    void createCommentShouldRejectWhenRateLimited() {
        when(articleMapper.selectById(ARTICLE_ID)).thenReturn(Article.builder()
                .id(ARTICLE_ID)
                .status(ArticleStatus.PUBLISHED)
                .allowComment(true)
                .build());
        when(userMapper.selectById(USER_ID)).thenReturn(User.builder()
                .id(USER_ID)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
        when(valueOperations.setIfAbsent(any(), any(), any(Duration.class))).thenReturn(false);

        BizException exception = assertThrows(BizException.class,
                () -> commentService.createComment(ARTICLE_ID, USER_ID, CreateCommentRequestDTO.builder()
                        .content("这篇文章不错")
                        .build()));

        assertEquals(ResultCode.COMMENT_RATE_LIMITED, exception.getResultCode());
        verify(commentMapper, never()).insert(any(Comment.class));
    }

    @Test
    void createCommentShouldReleaseRateLimitWhenTransactionRollsBack() {
        String key = "blog:comment:rate:user:" + USER_ID + ":article:" + ARTICLE_ID;
        when(articleMapper.selectById(ARTICLE_ID)).thenReturn(Article.builder()
                .id(ARTICLE_ID)
                .status(ArticleStatus.PUBLISHED)
                .allowComment(true)
                .build());
        when(userMapper.selectById(USER_ID)).thenReturn(User.builder()
                .id(USER_ID)
                .username("alice")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
        when(commentMapper.insert(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(COMMENT_ID);
            return 1;
        });
        when(commentMapper.selectById(COMMENT_ID)).thenReturn(Comment.builder()
                .id(COMMENT_ID)
                .articleId(ARTICLE_ID)
                .userId(USER_ID)
                .status(CommentStatus.PENDING)
                .content("这篇文章不错")
                .createdAt(CREATED_AT)
                .build());

        TransactionSynchronizationManager.initSynchronization();
        try {
            commentService.createComment(ARTICLE_ID, USER_ID, CreateCommentRequestDTO.builder()
                    .content("这篇文章不错")
                    .build());

            ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
            verify(valueOperations).setIfAbsent(eq(key), tokenCaptor.capture(), any(Duration.class));

            List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
            assertEquals(1, synchronizations.size());
            synchronizations.getFirst().afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

            verify(stringRedisTemplate).execute(
                    any(RedisScript.class),
                    eq(List.of(key)),
                    eq(tokenCaptor.getValue())
            );
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void deleteOwnCommentShouldRejectOtherUserComment() {
        when(commentMapper.selectById(COMMENT_ID)).thenReturn(Comment.builder()
                .id(COMMENT_ID)
                .articleId(ARTICLE_ID)
                .userId(USER_ID)
                .status(CommentStatus.APPROVED)
                .build());

        BizException exception = assertThrows(BizException.class,
                () -> commentService.deleteOwnComment(COMMENT_ID, ADMIN_ID));

        assertEquals(ResultCode.COMMENT_NO_PERMISSION, exception.getResultCode());
        verify(commentMapper, never()).update(any());
    }

    @Test
    void deleteOwnCommentShouldReturnDeletedApprovedCount() {
        when(commentMapper.selectById(COMMENT_ID)).thenReturn(Comment.builder()
                .id(COMMENT_ID)
                .articleId(ARTICLE_ID)
                .userId(USER_ID)
                .status(CommentStatus.APPROVED)
                .build());
        when(commentMapper.selectByIdForUpdate(COMMENT_ID)).thenReturn(Comment.builder()
                .id(COMMENT_ID)
                .articleId(ARTICLE_ID)
                .userId(USER_ID)
                .status(CommentStatus.APPROVED)
                .build());
        when(commentMapper.selectCommentSubtreeIds(COMMENT_ID)).thenReturn(List.of(COMMENT_ID, COMMENT_ID + 1));
        when(commentMapper.countApprovedCommentSubtree(COMMENT_ID)).thenReturn(2L);
        when(commentMapper.update(any(Wrapper.class))).thenReturn(1);

        CommentDeleteVO result = commentService.deleteOwnComment(COMMENT_ID, USER_ID);

        assertEquals(2L, result.getDeletedApprovedCount());
        verify(commentMapper).selectByIdForUpdate(COMMENT_ID);
        verify(articleMapper).update(any(), any(Wrapper.class));
    }
}
