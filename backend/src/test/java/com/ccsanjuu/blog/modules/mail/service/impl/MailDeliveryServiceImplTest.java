package com.ccsanjuu.blog.modules.mail.service.impl;

import com.ccsanjuu.blog.modules.comment.service.CommentReplyNotificationService;
import com.ccsanjuu.blog.modules.mail.mapper.MailDeliveryMapper;
import com.ccsanjuu.blog.modules.mail.model.entity.MailDelivery;
import com.ccsanjuu.blog.modules.mail.model.dto.MailDeliveryQueryDTO;
import com.ccsanjuu.blog.modules.mail.model.vo.MailDeliveryItemVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import com.ccsanjuu.blog.modules.mail.model.enums.MailDeliveryStatus;
import com.ccsanjuu.blog.modules.mail.model.enums.MailType;
import com.ccsanjuu.blog.modules.message.service.MessageReplyNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class MailDeliveryServiceImplTest {

    @BeforeAll
    static void initTableInfo() {
        if (TableInfoHelper.getTableInfo(MailDelivery.class) == null) {
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            assistant.setCurrentNamespace(MailDelivery.class.getName());
            TableInfoHelper.initTableInfo(assistant, MailDelivery.class);
        }
    }

    @Mock
    private MailDeliveryMapper mailDeliveryMapper;

    @Mock
    private ObjectProvider<MessageReplyNotificationService> messageServiceProvider;

    @Mock
    private ObjectProvider<CommentReplyNotificationService> commentServiceProvider;

    @Mock
    private CommentReplyNotificationService commentService;

    @Test
    void shouldStoreFullRecipientAndTrustedUserIdWithoutInferringGuests() {
        MailDeliveryServiceImpl service = new MailDeliveryServiceImpl(
                mailDeliveryMapper, messageServiceProvider, commentServiceProvider);

        var userDelivery = service.createPending("COMMENT_REPLY", 10L, 11L, "recipient@example.com", 10002L);
        var guestDelivery = service.createPending("MESSAGE_REPLY", 12L, 13L, "recipient@example.com", null);

        assertEquals("recipient@example.com", userDelivery.getRecipientEmail());
        assertEquals(10002L, userDelivery.getRecipientUserId());
        assertNull(guestDelivery.getRecipientUserId());
    }

    @Test
    void shouldReturnTheSameJoinedProfileInListAndDetail() {
        MailDeliveryItemVO item = MailDeliveryItemVO.builder().id(1L)
                .recipientEmail("original@example.com").userId(10002L)
                .userUsername("reader").userNickname("读者").userDeleted(false).build();
        when(mailDeliveryMapper.selectAdminPage(any(), any())).thenAnswer(invocation -> {
            Page<MailDeliveryItemVO> page = invocation.getArgument(0);
            page.setTotal(1);
            page.setRecords(List.of(item));
            return page;
        });
        when(mailDeliveryMapper.selectAdminDetail(1L)).thenReturn(item);
        MailDeliveryServiceImpl service = new MailDeliveryServiceImpl(
                mailDeliveryMapper, messageServiceProvider, commentServiceProvider);

        assertEquals(item, service.getPage(new MailDeliveryQueryDTO()).getRecords().getFirst());
        assertEquals(item, service.getDetail(1L));
    }

    @Test
    void shouldUpdateTheRecipientBeforeRetryWithoutReplacingOtherDeliveryFields() {
        MailDeliveryServiceImpl service = new MailDeliveryServiceImpl(
                mailDeliveryMapper, messageServiceProvider, commentServiceProvider);

        service.updateRecipient(1L, "new@example.com");

        ArgumentCaptor<MailDelivery> captor = ArgumentCaptor.forClass(MailDelivery.class);
        verify(mailDeliveryMapper).update(captor.capture(), any());
        assertEquals("new@example.com", captor.getValue().getRecipientEmail());
        assertNull(captor.getValue().getStatus());
        assertNull(captor.getValue().getRecipientUserId());
    }

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

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void retryShouldDispatchOnlyAfterStatusTransactionCommits(boolean commit) {
        MailDelivery delivery = MailDelivery.builder().id(1L).mailType(MailType.COMMENT_REPLY)
                .status(MailDeliveryStatus.FAILED).build();
        when(mailDeliveryMapper.selectByIdForUpdate(1L)).thenReturn(delivery);
        if (commit) when(commentServiceProvider.getObject()).thenReturn(commentService);
        MailDeliveryServiceImpl service = new MailDeliveryServiceImpl(
                mailDeliveryMapper, messageServiceProvider, commentServiceProvider);

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.retry(1L);
            verify(commentService, never()).retry(any());
            if (commit) {
                TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
                verify(commentService).retry(delivery);
            }
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
        if (!commit) verify(commentService, never()).retry(any());
    }
}
