package com.ccsanjuu.blog.modules.security.service;

import com.ccsanjuu.blog.modules.auth.mapper.AuthMapper;
import com.ccsanjuu.blog.modules.security.mapper.SecurityEventMapper;
import com.ccsanjuu.blog.properties.SecurityMaintenanceProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityMaintenanceTask {

    private static final int BATCH_SIZE = 1000;
    private static final int MAX_BATCHES = 100;

    private final SecurityEventMapper securityEventMapper;
    private final AuthMapper authMapper;
    private final SecurityMaintenanceProperties properties;

    /**
     * 每日分批清理安全数据，避免把全部历史删除放入一个长事务。
     */
    @Scheduled(cron = "${blog.security-maintenance.cron:0 30 3 * * *}", zone = "UTC")
    public void cleanExpiredData() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        int events = 0;
        int sessions = 0;
        for (int batch = 0; batch < MAX_BATCHES; batch++) {
            int deleted = securityEventMapper.deleteExpiredBatch(now.minusDays(properties.getEventRetentionDays()), BATCH_SIZE);
            events += deleted;
            if (deleted < BATCH_SIZE) break;
        }
        for (int batch = 0; batch < MAX_BATCHES; batch++) {
            int deleted = authMapper.deleteExpiredBatch(now.minusDays(properties.getSessionRetentionDays()), BATCH_SIZE);
            sessions += deleted;
            if (deleted < BATCH_SIZE) break;
        }
        log.info("security_event=SECURITY_DATA_CLEANUP events={} sessions={}", events, sessions);
    }
}
