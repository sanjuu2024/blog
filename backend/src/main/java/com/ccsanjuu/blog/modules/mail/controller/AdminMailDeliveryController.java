package com.ccsanjuu.blog.modules.mail.controller;

import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.modules.audit.annotation.AdminAudit;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditAction;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditResourceType;
import com.ccsanjuu.blog.modules.mail.model.dto.MailDeliveryQueryDTO;
import com.ccsanjuu.blog.modules.mail.model.vo.MailDeliveryItemVO;
import com.ccsanjuu.blog.modules.mail.service.MailDeliveryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/mail-deliveries")
@Validated
@RequiredArgsConstructor
public class AdminMailDeliveryController {
    private final MailDeliveryService mailDeliveryService;

    /**
     * 查询后台邮件投递分页记录。
     *
     * @param queryDTO 查询条件
     * @return 投递记录分页结果
     */
    @GetMapping
    public Result<PageResult<MailDeliveryItemVO>> getPage(@Valid @ModelAttribute MailDeliveryQueryDTO queryDTO) {
        return Result.success(mailDeliveryService.getPage(queryDTO));
    }

    /**
     * 查询后台邮件投递详情。
     *
     * @param deliveryId 投递记录 ID
     * @return 投递详情
     */
    @GetMapping("/{deliveryId}")
    public Result<MailDeliveryItemVO> getDetail(@PathVariable @Positive Long deliveryId) {
        return Result.success(mailDeliveryService.getDetail(deliveryId));
    }

    /**
     * 手动重试失败的回复通知邮件。
     *
     * @param deliveryId 投递记录 ID
     * @return 空响应
     */
    @PostMapping("/{deliveryId}/retry")
    @AdminAudit(resourceType = AdminAuditResourceType.NOTIFICATION, action = AdminAuditAction.RETRY,
            resourceId = "#p0")
    public Result<Void> retry(@PathVariable @Positive Long deliveryId) {
        mailDeliveryService.retry(deliveryId);
        return Result.success(null);
    }
}
