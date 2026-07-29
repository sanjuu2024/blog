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
import com.ccsanjuu.blog.modules.comment.mapper.CommentMapper;
import com.ccsanjuu.blog.modules.comment.model.dto.AdminCommentQueryDTO;
import com.ccsanjuu.blog.modules.comment.model.dto.CommentModerationRequestDTO;
import com.ccsanjuu.blog.modules.comment.model.entity.Comment;
import com.ccsanjuu.blog.modules.comment.model.enums.CommentModerationAction;
import com.ccsanjuu.blog.modules.comment.model.enums.CommentStatus;
import com.ccsanjuu.blog.modules.comment.model.enums.CommentType;
import com.ccsanjuu.blog.modules.comment.model.vo.AdminCommentItemVO;
import com.ccsanjuu.blog.modules.comment.model.vo.CommentMutationVO;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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

    private CommentServiceImpl commentService;

    @BeforeAll
    static void initMybatisPlusTableInfo() {
        initTableInfo(Comment.class);
    }

    @BeforeEach
    void setUp() {
        commentService = new CommentServiceImpl(articleMapper, commentMapper, userMapper);
        ReflectionTestUtils.setField(commentService, "baseMapper", commentMapper);
        ReflectionTestUtils.setField(commentService, "entityClass", Comment.class);
        ReflectionTestUtils.setField(commentService, "mapperClass", CommentMapper.class);
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
        when(commentMapper.update(any(Wrapper.class))).thenReturn(1);

        CommentModerationRequestDTO request = CommentModerationRequestDTO.builder()
                .action(CommentModerationAction.APPROVE)
                .build();

        CommentMutationVO result = commentService.moderateComment(COMMENT_ID, ADMIN_ID, request);

        assertEquals(CommentStatus.APPROVED, result.getStatus());
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
        when(commentMapper.selectById(COMMENT_ID)).thenReturn(Comment.builder()
                .id(COMMENT_ID)
                .articleId(ARTICLE_ID)
                .userId(USER_ID)
                .status(CommentStatus.APPROVED)
                .content("这篇文章不错")
                .createdAt(CREATED_AT)
                .build());

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
        when(commentMapper.selectById(COMMENT_ID)).thenReturn(Comment.builder()
                .id(COMMENT_ID)
                .articleId(ARTICLE_ID)
                .userId(USER_ID)
                .status(CommentStatus.APPROVED)
                .content("这篇文章不错")
                .createdAt(CREATED_AT)
                .build());

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
        when(commentMapper.selectList(any())).thenReturn(
                List.of(Comment.builder().id(COMMENT_ID + 1).parentId(COMMENT_ID).build()),
                List.of(Comment.builder().id(COMMENT_ID + 2).parentId(COMMENT_ID + 1).build()),
                List.of()
        );
        when(commentMapper.selectCount(any())).thenReturn(2L);
        when(commentMapper.update(any(Wrapper.class))).thenReturn(1);

        CommentModerationRequestDTO request = CommentModerationRequestDTO.builder()
                .action(CommentModerationAction.DELETE)
                .reason("删除测试")
                .build();

        CommentMutationVO result = commentService.moderateComment(COMMENT_ID, ADMIN_ID, request);

        assertEquals(CommentStatus.DELETED, result.getStatus());
        ArgumentCaptor<Wrapper<Comment>> captor = ArgumentCaptor.forClass(Wrapper.class);
        verify(commentMapper, times(2)).update(captor.capture());
        String sqlSet = ((LambdaUpdateWrapper<Comment>) captor.getAllValues().getFirst()).getSqlSet();
        assertTrue(sqlSet.contains("deleted_by"));
        assertTrue(sqlSet.contains("deleted_at"));
        verify(articleMapper).update(any(), any(Wrapper.class));
    }
}
