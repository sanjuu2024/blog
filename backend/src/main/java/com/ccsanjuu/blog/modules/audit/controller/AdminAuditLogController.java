package com.ccsanjuu.blog.modules.audit.controller;

import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.modules.audit.model.dto.AdminAuditLogQueryDTO;
import com.ccsanjuu.blog.modules.audit.model.vo.AdminAuditLogItemVO;
import com.ccsanjuu.blog.modules.audit.service.AdminAuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/audit-logs")
@Validated
@Tag(name = "后台审计日志接口")
@RequiredArgsConstructor
public class AdminAuditLogController {

    private final AdminAuditLogService adminAuditLogService;

    /**
     * 获取后台操作审计日志分页列表。
     *
     * @param queryDTO 查询条件
     * @return 审计日志分页结果
     */
    @GetMapping
    @Operation(description = "获取后台操作审计日志分页列表")
    public Result<PageResult<AdminAuditLogItemVO>> getAuditLogList(
            @Valid @ModelAttribute AdminAuditLogQueryDTO queryDTO
    ) {
        return Result.success(adminAuditLogService.getAuditLogList(queryDTO));
    }
}
