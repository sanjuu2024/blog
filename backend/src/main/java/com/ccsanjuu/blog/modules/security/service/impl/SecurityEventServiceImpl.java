package com.ccsanjuu.blog.modules.security.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.http.useragent.UserAgent;
import cn.hutool.http.useragent.UserAgentUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.modules.security.mapper.SecurityEventMapper;
import com.ccsanjuu.blog.modules.security.model.bo.SecurityEventRecordBO;
import com.ccsanjuu.blog.modules.security.model.dto.SecurityEventQueryDTO;
import com.ccsanjuu.blog.modules.security.model.entity.SecurityEvent;
import com.ccsanjuu.blog.modules.security.model.enums.SecurityEventOutcome;
import com.ccsanjuu.blog.modules.security.model.vo.SecurityEventItemVO;
import com.ccsanjuu.blog.modules.security.service.SecurityEventService;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityEventServiceImpl implements SecurityEventService {

    private final SecurityEventMapper securityEventMapper;
    private final PlatformTransactionManager transactionManager;
    private final UserMapper userMapper;

    /**
     * 使用独立事务追加安全事件；写入失败只记录应用日志，不影响原业务结果。
     *
     * @param record 安全事件
     */
    @Override
    public void record(SecurityEventRecordBO record) {
        HttpServletRequest request = currentRequest();
        // 在业务调用时保存请求信息，提交回调不再依赖可能已清理的请求上下文。
        SecurityEvent event = SecurityEvent.builder()
                .eventType(record.eventType())
                .outcome(record.outcome())
                .userId(record.userId())
                .actorId(record.actorId() != null && !record.actorId().equals(record.userId())
                        ? record.actorId() : null)
                .account(record.account())
                .ip(request == null ? null : request.getRemoteAddr())
                .userAgent(request == null ? null : request.getHeader("User-Agent"))
                .requestMethod(request == null ? null : request.getMethod())
                .requestPath(request == null ? null : request.getRequestURI())
                .description(record.description())
                .build();
        if (record.outcome() == SecurityEventOutcome.SUCCESS
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    persist(event);
                }
            });
            return;
        }
        persist(event);
    }

    /**
     * 按筛选条件分页查询安全事件。
     *
     * @param queryDTO 查询条件
     * @return 安全事件分页结果
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<SecurityEventItemVO> getSecurityEventList(SecurityEventQueryDTO queryDTO) {
        Page<SecurityEvent> page = Page.of(queryDTO.getPageNum(), queryDTO.getPageSize());
        securityEventMapper.selectPage(page, new LambdaQueryWrapper<SecurityEvent>()
                .eq(queryDTO.getEventType() != null, SecurityEvent::getEventType, queryDTO.getEventType())
                .eq(queryDTO.getOutcome() != null, SecurityEvent::getOutcome, queryDTO.getOutcome())
                .eq(queryDTO.getUserId() != null, SecurityEvent::getUserId, queryDTO.getUserId())
                .ge(queryDTO.getCreatedAtFrom() != null, SecurityEvent::getCreatedAt, queryDTO.getCreatedAtFrom())
                .le(queryDTO.getCreatedAtTo() != null, SecurityEvent::getCreatedAt, queryDTO.getCreatedAtTo())
                .orderByDesc(SecurityEvent::getCreatedAt)
                .orderByDesc(SecurityEvent::getId));
        var records = BeanUtil.copyToList(page.getRecords(), SecurityEventItemVO.class);
        // 合并本页目标用户和操作者，批量读取当前资料，避免逐条查询和存储重复快照。
        var userIds = new HashSet<Long>();
        for (SecurityEventItemVO item : records) {
            if (item.getUserId() != null) {
                userIds.add(item.getUserId());
            }
            if (item.getActorId() != null) {
                userIds.add(item.getActorId());
            }
        }
        Map<Long, User> users = userIds.isEmpty() ? Map.of() : userMapper.selectList(
                new LambdaQueryWrapper<User>()
                        .select(User::getId, User::getUsername, User::getNickname, User::getDeletedAt)
                        .in(User::getId, userIds)
        ).stream().collect(Collectors.toMap(User::getId, user -> user));
        for (SecurityEventItemVO item : records) {
            User user = item.getUserId() == null ? null : users.get(item.getUserId());
            if (user != null) {
                item.setUserDeleted(user.getDeletedAt() != null);
                item.setUserUsername(user.getUsername());
                if (!item.getUserDeleted()) {
                    item.setUserNickname(user.getNickname());
                }
            }
            User actor = item.getActorId() == null ? null : users.get(item.getActorId());
            if (actor != null) {
                item.setActorDeleted(actor.getDeletedAt() != null);
                item.setActorUsername(actor.getUsername());
                if (!item.getActorDeleted()) {
                    item.setActorNickname(actor.getNickname());
                }
            }
            if (item.getUserAgent() != null && !item.getUserAgent().isBlank()) {
                UserAgent userAgent = UserAgentUtil.parse(item.getUserAgent());
                item.setBrowser(userAgent.getBrowser().getName());
                item.setOperatingSystem(userAgent.getOs().getName());
                item.setDevice(userAgent.getPlatform().isUnknown() ? "未知设备"
                        : userAgent.isMobile() ? "移动设备" : "桌面设备");
            }
        }
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    /**
     * 获取当前请求，仅使用容器与可信代理配置处理后的来源信息。
     *
     * @return 当前请求，无请求上下文时为空
     */
    private HttpServletRequest currentRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes
                ? attributes.getRequest() : null;
    }

    /**
     * 独立提交事件；失败时不回滚已经完成的业务操作。
     *
     * @param event 安全事件及请求信息快照
     */
    private void persist(SecurityEvent event) {
        try {
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
            transactionTemplate.setPropagationBehavior(Propagation.REQUIRES_NEW.value());
            transactionTemplate.executeWithoutResult(status -> securityEventMapper.insert(event));
        } catch (RuntimeException exception) {
            log.error("security_event=SECURITY_EVENT_PERSIST_FAILED outcome=FAIL eventType={} reason={}",
                    event.getEventType(), exception.getClass().getSimpleName());
        }
    }
}
