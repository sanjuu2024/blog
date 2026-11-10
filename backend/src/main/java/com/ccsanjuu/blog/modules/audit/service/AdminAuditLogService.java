package com.ccsanjuu.blog.modules.audit.service;

import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.modules.audit.model.bo.AdminAuditRecordBO;
import com.ccsanjuu.blog.modules.audit.model.dto.AdminAuditLogQueryDTO;
import com.ccsanjuu.blog.modules.audit.model.vo.AdminAuditLogItemVO;

public interface AdminAuditLogService {

    /**
     * 获取后台操作审计日志分页列表。
     *
     * @param queryDTO 查询条件
     * @return 审计日志分页结果
     */
    PageResult<AdminAuditLogItemVO> getAuditLogList(AdminAuditLogQueryDTO queryDTO);

    /**
     * 在当前后台业务事务中追加成功审计日志。
     *
     * @param record 审计记录
     */
    void recordSuccess(AdminAuditRecordBO record);

    /**
     * 在业务事务回滚后使用独立事务追加失败审计日志。
     *
     * @param record 失败审计记录
     */
    void recordFailure(AdminAuditRecordBO record);
}
