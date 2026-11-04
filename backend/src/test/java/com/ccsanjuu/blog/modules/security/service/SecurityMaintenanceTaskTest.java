package com.ccsanjuu.blog.modules.security.service;

import com.ccsanjuu.blog.modules.auth.mapper.AuthMapper;
import com.ccsanjuu.blog.modules.security.mapper.SecurityEventMapper;
import com.ccsanjuu.blog.properties.SecurityMaintenanceProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityMaintenanceTaskTest {

    @Mock
    private SecurityEventMapper securityEventMapper;

    @Mock
    private AuthMapper authMapper;

    @Test
    void shouldUseRetentionCutoffsAndStopAfterTheLastPartialBatch() {
        when(securityEventMapper.deleteExpiredBatch(any(), eq(1000))).thenReturn(1000, 2);
        OffsetDateTime before = OffsetDateTime.now(ZoneOffset.UTC);
        new SecurityMaintenanceTask(securityEventMapper, authMapper, new SecurityMaintenanceProperties()).cleanExpiredData();
        OffsetDateTime after = OffsetDateTime.now(ZoneOffset.UTC);

        ArgumentCaptor<OffsetDateTime> eventCutoff = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(securityEventMapper, times(2)).deleteExpiredBatch(eventCutoff.capture(), eq(1000));
        assertTrue(!eventCutoff.getValue().isBefore(before.minusDays(90))
                && !eventCutoff.getValue().isAfter(after.minusDays(90)));
        ArgumentCaptor<OffsetDateTime> sessionCutoff = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(authMapper).deleteExpiredBatch(sessionCutoff.capture(), eq(1000));
        assertTrue(!sessionCutoff.getValue().isBefore(before.minusDays(7))
                && !sessionCutoff.getValue().isAfter(after.minusDays(7)));
    }

    @Test
    void shouldBoundEachRunEvenWhenHistoryRemains() {
        when(securityEventMapper.deleteExpiredBatch(any(), eq(1000))).thenReturn(1000);
        when(authMapper.deleteExpiredBatch(any(), eq(1000))).thenReturn(1000);

        new SecurityMaintenanceTask(securityEventMapper, authMapper, new SecurityMaintenanceProperties()).cleanExpiredData();

        verify(securityEventMapper, times(100)).deleteExpiredBatch(any(), eq(1000));
        verify(authMapper, times(100)).deleteExpiredBatch(any(), eq(1000));
    }
}
