package com.ccsanjuu.blog.modules.audit.model.bo;

import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditAction;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditResourceType;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditResult;

/**
 * 一次后台管理操作的安全审计记录。
 *
 * @param operatorId 操作者用户 ID
 * @param operatorUsername 操作者用户名快照
 * @param resourceType 目标资源类型
 * @param resourceId 目标资源标识，批量操作可以保存多个 ID
 * @param action 操作类型
 * @param actionDetail 状态、审核动作或上传场景等非敏感明细
 * @param result 操作结果
 * @param failureCode 失败业务码，成功时为空
 * @param failureMessage 失败原因，成功时为空
 * @param requestMethod HTTP 请求方法
 * @param requestPath HTTP 请求路径，不包含查询参数
 */
public record AdminAuditRecordBO(
        Long operatorId,
        String operatorUsername,
        AdminAuditResourceType resourceType,
        String resourceId,
        AdminAuditAction action,
        String actionDetail,
        AdminAuditResult result,
        Integer failureCode,
        String failureMessage,
        String requestMethod,
        String requestPath
) {
}
