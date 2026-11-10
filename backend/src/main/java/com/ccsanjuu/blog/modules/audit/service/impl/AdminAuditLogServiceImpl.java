package com.ccsanjuu.blog.modules.audit.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.modules.audit.mapper.AdminAuditLogMapper;
import com.ccsanjuu.blog.modules.audit.model.bo.AdminAuditRecordBO;
import com.ccsanjuu.blog.modules.audit.model.dto.AdminAuditLogQueryDTO;
import com.ccsanjuu.blog.modules.audit.model.entity.AdminAuditLog;
import com.ccsanjuu.blog.modules.audit.model.vo.AdminAuditLogItemVO;
import com.ccsanjuu.blog.modules.audit.service.AdminAuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAuditLogServiceImpl implements AdminAuditLogService {

    private final AdminAuditLogMapper adminAuditLogMapper;

    /**
     * 获取后台操作审计日志分页列表。
     *
     * @param queryDTO 查询条件
     * @return 审计日志分页结果
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminAuditLogItemVO> getAuditLogList(AdminAuditLogQueryDTO queryDTO) {
        Page<AdminAuditLog> page = Page.of(queryDTO.getPageNum(), queryDTO.getPageSize());
        String resourceId = StringUtils.hasText(queryDTO.getResourceId())
                ? queryDTO.getResourceId().trim()
                : null;
        LambdaQueryWrapper<AdminAuditLog> wrapper = new LambdaQueryWrapper<AdminAuditLog>()
                .eq(queryDTO.getOperatorId() != null, AdminAuditLog::getOperatorId, queryDTO.getOperatorId())
                .eq(queryDTO.getResourceType() != null, AdminAuditLog::getResourceType, queryDTO.getResourceType())
                .like(StringUtils.hasText(resourceId), AdminAuditLog::getResourceId, resourceId)
                .eq(queryDTO.getAction() != null, AdminAuditLog::getAction, queryDTO.getAction())
                .eq(queryDTO.getResult() != null, AdminAuditLog::getResult, queryDTO.getResult())
                .ge(queryDTO.getCreatedAtFrom() != null, AdminAuditLog::getCreatedAt, queryDTO.getCreatedAtFrom())
                .le(queryDTO.getCreatedAtTo() != null, AdminAuditLog::getCreatedAt, queryDTO.getCreatedAtTo())
                .orderByDesc(AdminAuditLog::getCreatedAt)
                .orderByDesc(AdminAuditLog::getId);
        adminAuditLogMapper.selectPage(page, wrapper);

        List<AdminAuditLogItemVO> records = BeanUtil.copyToList(page.getRecords(), AdminAuditLogItemVO.class);
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    /**
     * 在当前后台业务事务中追加成功审计日志，确保业务数据与日志同时提交。
     *
     * @param record 审计记录
     */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordSuccess(AdminAuditRecordBO record) {
        insertAuditLog(record);
    }

    /**
     * 在业务事务回滚后使用独立事务追加失败审计日志。
     *
     * @param record 失败审计记录
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(AdminAuditRecordBO record) {
        insertAuditLog(record);
    }

    /**
     * 将审计业务对象转换为只追加的持久化实体。
     *
     * @param record 审计记录
     */
    private void insertAuditLog(AdminAuditRecordBO record) {
        adminAuditLogMapper.insert(AdminAuditLog.builder()
                .operatorId(record.operatorId())
                .operatorUsername(record.operatorUsername())
                .resourceType(record.resourceType())
                .resourceId(record.resourceId())
                .action(record.action())
                .actionDetail(record.actionDetail())
                .result(record.result())
                .failureCode(record.failureCode())
                .failureMessage(record.failureMessage())
                .requestMethod(record.requestMethod())
                .requestPath(record.requestPath())
                .build());
    }
}
