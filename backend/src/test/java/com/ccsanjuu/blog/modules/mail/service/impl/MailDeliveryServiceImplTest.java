package com.ccsanjuu.blog.modules.mail.service.impl;

import com.ccsanjuu.blog.modules.comment.service.CommentReplyNotificationService;
import com.ccsanjuu.blog.modules.mail.mapper.MailDeliveryMapper;
import com.ccsanjuu.blog.modules.mail.model.entity.MailDelivery;
import com.ccsanjuu.blog.modules.mail.model.enums.MailDeliveryStatus;
import com.ccsanjuu.blog.modules.mail.model.enums.MailType;
import com.ccsanjuu.blog.modules.message.service.MessageReplyNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailDeliveryServiceImplTest {

    @Mock
    private MailDeliveryMapper mailDeliveryMapper;

    @Mock
    private ObjectProvider<MessageReplyNotificationService> messageServiceProvider;

    @Mock
    private ObjectProvider<CommentReplyNotificationService> commentServiceProvider;

    @Mock
    private CommentReplyNotificationService commentService;

    @Test
    void retryShouldDispatchCommentReplyDelivery() {
        MailDelivery delivery = MailDelivery.builder()
                .id(1L)
                .mailType(MailType.COMMENT_REPLY)
                .status(MailDeliveryStatus.FAILED)
                .build();
        when(mailDeliveryMapper.selectByIdForUpdate(1L)).thenReturn(delivery);
        when(commentServiceProvider.getObject()).thenReturn(commentService);
        MailDeliveryServiceImpl service = new MailDeliveryServiceImpl(
                mailDeliveryMapper, messageServiceProvider, commentServiceProvider);

        service.retry(1L);

        verify(commentService).retry(delivery);
    }
}
