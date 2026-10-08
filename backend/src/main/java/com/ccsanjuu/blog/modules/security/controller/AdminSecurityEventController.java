package com.ccsanjuu.blog.modules.security.controller;

import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.modules.security.model.dto.SecurityEventQueryDTO;
import com.ccsanjuu.blog.modules.security.model.vo.SecurityEventItemVO;
import com.ccsanjuu.blog.modules.security.service.SecurityEventService;
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
@RequestMapping("/admin/security-events")
@Validated
@Tag(name = "后台安全事件接口")
@RequiredArgsConstructor
public class AdminSecurityEventController {

    private final SecurityEventService securityEventService;

    /**
     * 获取安全事件分页列表。
     *
     * @param queryDTO 查询条件
     * @return 安全事件分页结果
     */
    @GetMapping
    @Operation(description = "获取安全事件分页列表")
    public Result<PageResult<SecurityEventItemVO>> getSecurityEventList(
            @Valid @ModelAttribute SecurityEventQueryDTO queryDTO
    ) {
        return Result.success(securityEventService.getSecurityEventList(queryDTO));
    }
}
