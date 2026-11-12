package com.ccsanjuu.blog.modules.comment.service.impl;

import com.ccsanjuu.blog.modules.article.mapper.ArticleMapper;
import com.ccsanjuu.blog.modules.article.model.entity.Article;
import com.ccsanjuu.blog.modules.article.model.enums.ArticleStatus;
import com.ccsanjuu.blog.modules.category.mapper.CategoryMapper;
import com.ccsanjuu.blog.modules.category.model.entity.Category;
import com.ccsanjuu.blog.modules.category.model.enums.CategoryStatus;
import com.ccsanjuu.blog.modules.comment.mapper.CommentMapper;
import com.ccsanjuu.blog.modules.comment.model.entity.Comment;
import com.ccsanjuu.blog.modules.comment.model.enums.CommentStatus;
import com.ccsanjuu.blog.modules.mail.model.bo.MailMessageContent;
import com.ccsanjuu.blog.modules.mail.model.entity.MailDelivery;
import com.ccsanjuu.blog.modules.mail.service.MailDeliveryService;
import com.ccsanjuu.blog.modules.mail.service.MailNotificationSender;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import com.ccsanjuu.blog.properties.BlogProperties;
import com.ccsanjuu.blog.properties.BlogMailProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.function.Supplier;
import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentReplyNotificationServiceImplTest {

    @Mock
    private MailDeliveryService mailDeliveryService;
    @Mock
    private MailNotificationSender mailNotificationSender;
    @Mock
    private CommentMapper commentMapper;
    @Mock
    private ArticleMapper articleMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private CategoryMapper categoryMapper;

    private CommentReplyNotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() throws java.io.IOException {
        BlogMailProperties properties = new BlogMailProperties();
        properties.setEnabled(true);
        properties.setFrontendBaseUrl("https://blog.example.com");
        notificationService = new CommentReplyNotificationServiceImpl(
                properties, new BlogProperties(), mailDeliveryService, mailNotificationSender,
                commentMapper, new CommentStickerServiceImpl(new com.fasterxml.jackson.databind.ObjectMapper()), articleMapper, userMapper, categoryMapper);
        org.mockito.Mockito.lenient().when(userMapper.selectById(10001L)).thenReturn(User.builder()
                .id(10001L).email("alice@example.com").emailVerified(true).build());
        org.mockito.Mockito.lenient().when(userMapper.selectById(10002L)).thenReturn(User.builder()
                .id(10002L).nickname("Bob").build());
        org.mockito.Mockito.lenient().when(commentMapper.selectById(60001L)).thenReturn(Comment.builder()
                .id(60001L).articleId(40001L).userId(10001L).status(CommentStatus.APPROVED).notifyOnReply(true)
                .unsubscribeToken("comment-token").content("<原评论>").build());
        org.mockito.Mockito.lenient().when(commentMapper.selectById(60002L)).thenReturn(Comment.builder()
                .id(60002L).parentId(60001L).status(CommentStatus.APPROVED).userId(10002L).articleId(40001L).content("直接回复").build());
        org.mockito.Mockito.lenient().when(articleMapper.selectById(40001L)).thenReturn(Article.builder()
                .id(40001L).categoryId(20002L).status(ArticleStatus.PUBLISHED).title("测试文章").build());
        org.mockito.Mockito.lenient().when(categoryMapper.selectById(20002L)).thenReturn(Category.builder()
                .id(20002L).parentId(20001L).status(CategoryStatus.ENABLED).build());
        org.mockito.Mockito.lenient().when(categoryMapper.selectById(20001L)).thenReturn(Category.builder()
                .id(20001L).status(CategoryStatus.ENABLED).build());
        org.mockito.Mockito.lenient().when(mailDeliveryService.createPending(any(), any(), any(), any(), any()))
                .thenReturn(MailDelivery.builder().id(1L).build());
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void subscribedDirectReplyShouldSubmitAfterCommitAndEscapeHtml() {
        TransactionSynchronizationManager.initSynchronization();
        notificationService.sendAfterCommit(
                commentMapper.selectById(60001L), commentMapper.selectById(60002L));

        verify(mailNotificationSender, never()).submit(any(), any());
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        ArgumentCaptor<Supplier<MailMessageContent>> captor = ArgumentCaptor.forClass(Supplier.class);
        verify(mailNotificationSender).submit(any(), captor.capture());
        MailMessageContent content = captor.getValue().get();
        assertTrue(content.html().contains("&lt;原评论&gt;"));
    }

    @Test
    void mailShouldUseTextForBothOriginalAndReplyStickers() {
        commentMapper.selectById(60001L).setContent("原文[sticker:bilibili:doge]");
        commentMapper.selectById(60002L).setContent("回复[sticker:wechat:微笑]");
        notificationService.sendAfterCommit(commentMapper.selectById(60001L), commentMapper.selectById(60002L));
        ArgumentCaptor<Supplier<MailMessageContent>> captor = ArgumentCaptor.forClass(Supplier.class);
        verify(mailNotificationSender).submit(any(), captor.capture());
        MailMessageContent content = captor.getValue().get();
        assertTrue(content.html().contains("原文[doge]"));
        assertTrue(content.html().contains("回复[微笑]"));
        assertTrue(content.plainText().contains("原文[doge]"));
    }

    @Test
    void selfReplyShouldNotSubmitMail() {
        Comment reply = Comment.builder().id(60002L).userId(10001L).articleId(40001L).build();

        notificationService.sendAfterCommit(commentMapper.selectById(60001L), reply);

        verify(mailNotificationSender, never()).submit(any(), any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"unsubscribe", "hidden", "rootHidden", "offline", "category", "deleted", "unrelated"})
    void retryShouldRejectInvalidCurrentSubscriptionOrSource(String scenario) {
        switch (scenario) {
            case "unsubscribe" -> commentMapper.selectById(60001L).setNotifyOnReply(false);
            case "hidden" -> commentMapper.selectById(60002L).setStatus(CommentStatus.HIDDEN);
            case "rootHidden" -> {
                commentMapper.selectById(60001L).setRootId(60000L);
                when(commentMapper.selectById(60000L)).thenReturn(Comment.builder().id(60000L).status(CommentStatus.HIDDEN).build());
            }
            case "offline" -> articleMapper.selectById(40001L).setStatus(ArticleStatus.OFFLINE);
            case "category" -> categoryMapper.selectById(20001L).setStatus(CategoryStatus.DISABLED);
            case "deleted" -> userMapper.selectById(10001L).setDeletedAt(OffsetDateTime.now());
            case "unrelated" -> commentMapper.selectById(60002L).setParentId(60003L);
        }
        notificationService.retry(MailDelivery.builder().id(1L).sourceId(60001L).replyId(60002L).build());

        ArgumentCaptor<Supplier<MailMessageContent>> captor = ArgumentCaptor.forClass(Supplier.class);
        verify(mailNotificationSender).submit(any(), captor.capture());
        assertThrows(IllegalStateException.class, () -> captor.getValue().get());
    }

    @Test
    void queuedFirstAttemptShouldRecheckUnsubscribe() {
        notificationService.sendAfterCommit(commentMapper.selectById(60001L), commentMapper.selectById(60002L));
        commentMapper.selectById(60001L).setNotifyOnReply(false);

        ArgumentCaptor<Supplier<MailMessageContent>> captor = ArgumentCaptor.forClass(Supplier.class);
        verify(mailNotificationSender).submit(any(), captor.capture());
        assertThrows(IllegalStateException.class, () -> captor.getValue().get());
    }
}
