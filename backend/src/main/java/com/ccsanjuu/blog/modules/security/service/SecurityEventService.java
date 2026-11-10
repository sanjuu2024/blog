package com.ccsanjuu.blog.modules.security.service;

import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.modules.security.model.bo.SecurityEventRecordBO;
import com.ccsanjuu.blog.modules.security.model.dto.SecurityEventQueryDTO;
import com.ccsanjuu.blog.modules.security.model.vo.SecurityEventItemVO;

public interface SecurityEventService {

    /**
     * 追加一条安全事件。
     *
     * @param record 安全事件
     */
    void record(SecurityEventRecordBO record);

    /**
     * 获取安全事件分页列表。
     *
     * @param queryDTO 查询条件
     * @return 安全事件分页结果
     */
    PageResult<SecurityEventItemVO> getSecurityEventList(SecurityEventQueryDTO queryDTO);
}
