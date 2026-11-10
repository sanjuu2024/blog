package com.ccsanjuu.blog.modules.security.service.impl;

import com.ccsanjuu.blog.modules.security.mapper.SecurityEventMapper;
import com.ccsanjuu.blog.modules.security.model.bo.SecurityEventRecordBO;
import com.ccsanjuu.blog.modules.security.model.entity.SecurityEvent;
import com.ccsanjuu.blog.modules.security.model.enums.SecurityEventOutcome;
import com.ccsanjuu.blog.modules.security.model.enums.SecurityEventType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccsanjuu.blog.modules.security.model.dto.SecurityEventQueryDTO;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import java.util.List;
import java.time.OffsetDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class SecurityEventServiceImplTest {

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36";

    @Mock
    private SecurityEventMapper securityEventMapper;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private UserMapper userMapper;

    private SecurityEventServiceImpl service;

    @BeforeAll
    static void initTableInfo() {
        if (TableInfoHelper.getTableInfo(User.class) == null) {
            MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
            assistant.setCurrentNamespace(User.class.getName());
            TableInfoHelper.initTableInfo(assistant, User.class);
        }
    }

    @BeforeEach
    void setUp() {
        service = new SecurityEventServiceImpl(securityEventMapper, transactionManager, userMapper);
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }

    @AfterEach
    void clearRequestContext() {
        RequestContextHolder.resetRequestAttributes();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void recordShouldPersistReadableMetadataWithoutCredentialsOrQuery() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("192.0.2.10");
        request.addHeader("User-Agent", USER_AGENT);
        request.addHeader("X-Forwarded-For", "spoofed-ip");
        request.addHeader("Authorization", "Bearer secret-token");
        request.addHeader("Cookie", "refresh_token=secret-token");
        request.setQueryString("token=secret-token");
        request.setContent("password=secret-password".getBytes());
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        service.record(loginEvent(SecurityEventOutcome.FAILURE));

        ArgumentCaptor<SecurityEvent> captor = ArgumentCaptor.forClass(SecurityEvent.class);
        verify(securityEventMapper).insert(captor.capture());
        SecurityEvent event = captor.getValue();
        assertEquals(SecurityEventType.LOGIN, event.getEventType());
        assertEquals("alice@example.com", event.getAccount());
        assertEquals("192.0.2.10", event.getIp());
        assertEquals(USER_AGENT, event.getUserAgent());
        assertEquals("POST", event.getRequestMethod());
        assertEquals("/api/v1/auth/login", event.getRequestPath());
        assertNull(event.getActorId());
        assertFalse(event.toString().contains("secret-"));
    }

    @Test
    void successShouldPersistSnapshotOnlyAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        request.setRemoteAddr("192.0.2.10");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        service.record(loginEvent(SecurityEventOutcome.SUCCESS));
        verifyNoInteractions(securityEventMapper);
        RequestContextHolder.resetRequestAttributes();
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        ArgumentCaptor<SecurityEvent> captor = ArgumentCaptor.forClass(SecurityEvent.class);
        verify(securityEventMapper).insert(captor.capture());
        assertEquals("192.0.2.10", captor.getValue().getIp());
    }

    @Test
    void rolledBackSuccessShouldNotBePersisted() {
        TransactionSynchronizationManager.initSynchronization();
        service.record(loginEvent(SecurityEventOutcome.SUCCESS));

        TransactionSynchronizationManager.getSynchronizations().forEach(
                synchronization -> synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verifyNoInteractions(securityEventMapper);
    }

    @Test
    void failureShouldPersistImmediatelyEvenWithActiveSynchronization() {
        TransactionSynchronizationManager.initSynchronization();

        service.record(loginEvent(SecurityEventOutcome.FAILURE));

        verify(securityEventMapper).insert(any(SecurityEvent.class));
        assertEquals(0, TransactionSynchronizationManager.getSynchronizations().size());
    }

    @Test
    void persistenceFailureShouldNotChangeBusinessResult() {
        doThrow(new IllegalStateException("database unavailable"))
                .when(securityEventMapper).insert(any(SecurityEvent.class));

        assertDoesNotThrow(() -> service.record(loginEvent(SecurityEventOutcome.FAILURE)));
    }

    @Test
    void queryShouldParseClientAndPreserveMissingHistoricalMetadata() {
        when(securityEventMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<SecurityEvent> page = invocation.getArgument(0);
            page.setRecords(List.of(SecurityEvent.builder().id(1L).userAgent(USER_AGENT).build(),
                    SecurityEvent.builder().id(2L).build()));
            page.setTotal(2);
            return page;
        });

        var result = service.getSecurityEventList(new SecurityEventQueryDTO());

        assertEquals("Chrome", result.getRecords().getFirst().getBrowser());
        assertNotNull(result.getRecords().getFirst().getOperatingSystem());
        assertNotNull(result.getRecords().getFirst().getDevice());
        assertNull(result.getRecords().getLast().getUserAgent());
        assertNull(result.getRecords().getLast().getBrowser());
        verifyNoInteractions(userMapper);
    }

    @Test
    void queryShouldBatchLoadCurrentTargetAndActorProfiles() {
        when(securityEventMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<SecurityEvent> page = invocation.getArgument(0);
            page.setRecords(List.of(SecurityEvent.builder().id(1L).userId(8L).actorId(1L).build(),
                    SecurityEvent.builder().id(2L).userId(8L).build(),
                    SecurityEvent.builder().id(3L).userId(9L).actorId(10L).build()));
            return page;
        });
        when(userMapper.selectList(any())).thenReturn(List.of(
                User.builder().id(8L).username("test").nickname("测试用户").build(),
                User.builder().id(1L).username("admin").nickname("站点管理员").build(),
                User.builder().id(9L).username("deleted_9").nickname("旧昵称")
                        .deletedAt(OffsetDateTime.now()).build(),
                User.builder().id(10L).username("deleted_10").nickname("旧管理员")
                        .deletedAt(OffsetDateTime.now()).build()));

        var records = service.getSecurityEventList(new SecurityEventQueryDTO()).getRecords();

        assertEquals("test", records.getFirst().getUserUsername());
        assertEquals("测试用户", records.getFirst().getUserNickname());
        assertFalse(records.getFirst().getUserDeleted());
        assertEquals("admin", records.getFirst().getActorUsername());
        assertEquals("站点管理员", records.getFirst().getActorNickname());
        assertFalse(records.getFirst().getActorDeleted());
        assertNull(records.get(1).getActorNickname());
        assertTrue(records.getLast().getUserDeleted());
        assertEquals("deleted_9", records.getLast().getUserUsername());
        assertNull(records.getLast().getUserNickname());
        assertTrue(records.getLast().getActorDeleted());
        assertEquals("deleted_10", records.getLast().getActorUsername());
        assertNull(records.getLast().getActorNickname());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaQueryWrapper<User>> captor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        verify(userMapper, times(1)).selectList(captor.capture());
        captor.getValue().getSqlSegment();
        assertEquals(Set.of(1L, 8L, 9L, 10L), Set.copyOf(captor.getValue().getParamNameValuePairs().values()));
        assertFalse(captor.getValue().getSqlSelect().contains("password_hash"));
    }

    @Test
    void queryShouldKeepUnmatchedAccountWithoutGuessingUserIdentity() {
        when(securityEventMapper.selectPage(any(), any())).thenAnswer(invocation -> {
            Page<SecurityEvent> page = invocation.getArgument(0);
            page.setRecords(List.of(SecurityEvent.builder().id(1L).account("unknown@example.com").build(),
                    SecurityEvent.builder().id(2L).userId(8L).build()));
            return page;
        });
        when(userMapper.selectList(any())).thenReturn(List.of());

        var records = service.getSecurityEventList(new SecurityEventQueryDTO()).getRecords();

        assertEquals("unknown@example.com", records.getFirst().getAccount());
        assertNull(records.getFirst().getUserId());
        assertNull(records.getLast().getUserUsername());
        assertNull(records.getLast().getUserNickname());
        assertNull(records.getLast().getUserDeleted());
    }

    private SecurityEventRecordBO loginEvent(SecurityEventOutcome outcome) {
        return new SecurityEventRecordBO(SecurityEventType.LOGIN, outcome, 10001L, 10001L,
                "alice@example.com", "登录结果");
    }
}
