package com.ccsanjuu.blog.modules.security.model.bo;

import com.ccsanjuu.blog.modules.security.model.enums.SecurityEventOutcome;
import com.ccsanjuu.blog.modules.security.model.enums.SecurityEventType;

/**
 * 待追加的安全事件。
 *
 * @param eventType 事件类型
 * @param outcome 事件结果
 * @param userId 目标用户 ID
 * @param actorId 管理员操作其他用户时的操作者 ID
 * @param account 原始账号
 * @param description 非敏感描述
 */
public record SecurityEventRecordBO(
        SecurityEventType eventType,
        SecurityEventOutcome outcome,
        Long userId,
        Long actorId,
        String account,
        String description
) {
}
