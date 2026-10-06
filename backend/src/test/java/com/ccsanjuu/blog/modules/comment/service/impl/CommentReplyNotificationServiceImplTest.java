package com.ccsanjuu.blog.modules.comment.service.impl;

import com.ccsanjuu.blog.modules.article.mapper.ArticleMapper;
import com.ccsanjuu.blog.modules.article.model.entity.Article;
import com.ccsanjuu.blog.modules.comment.mapper.CommentMapper;
import com.ccsanjuu.blog.modules.comment.model.entity.Comment;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.function.Supplier;

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

    private CommentReplyNotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        BlogMailProperties properties = new BlogMailProperties();
        properties.setEnabled(true);
        properties.setFrontendBaseUrl("https://blog.example.com");
        notificationService = new CommentReplyNotificationServiceImpl(
                properties, new BlogProperties(), mailDeliveryService, mailNotificationSender,
                commentMapper, articleMapper, userMapper);
        org.mockito.Mockito.lenient().when(userMapper.selectById(10001L)).thenReturn(User.builder()
                .id(10001L).email("alice@example.com").emailVerified(true).build());
        org.mockito.Mockito.lenient().when(userMapper.selectById(10002L)).thenReturn(User.builder()
                .id(10002L).nickname("Bob").build());
        org.mockito.Mockito.lenient().when(commentMapper.selectById(60001L)).thenReturn(Comment.builder()
                .id(60001L).userId(10001L).notifyOnReply(true)
                .unsubscribeToken("comment-token").content("<原评论>").build());
        org.mockito.Mockito.lenient().when(commentMapper.selectById(60002L)).thenReturn(Comment.builder()
                .id(60002L).userId(10002L).articleId(40001L).content("直接回复").build());
        org.mockito.Mockito.lenient().when(articleMapper.selectById(40001L)).thenReturn(Article.builder().id(40001L).title("测试文章").build());
        org.mockito.Mockito.lenient().when(mailDeliveryService.createPending(any(), any(), any(), any()))
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
    void selfReplyShouldNotSubmitMail() {
        Comment reply = Comment.builder().id(60002L).userId(10001L).articleId(40001L).build();

        notificationService.sendAfterCommit(commentMapper.selectById(60001L), reply);

        verify(mailNotificationSender, never()).submit(any(), any());
    }
}
