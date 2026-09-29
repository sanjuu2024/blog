package com.ccsanjuu.blog.modules.notification.controller;

import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.modules.audit.annotation.AdminAudit;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditAction;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditResourceType;
import com.ccsanjuu.blog.modules.auth.model.security.JwtPrincipal;
import com.ccsanjuu.blog.modules.notification.model.dto.CreateAdminNotificationRequestDTO;
import com.ccsanjuu.blog.modules.notification.model.dto.UpdateAdminNotificationRequestDTO;
import com.ccsanjuu.blog.modules.notification.model.vo.AdminNotificationItemVO;
import com.ccsanjuu.blog.modules.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/notifications")
@Validated
@Tag(name = "管理员消息接口")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final NotificationService notificationService;

    /**
     * 获取管理员消息列表。
     *
     * @return 管理员消息列表
     */
    @GetMapping
    @Operation(description = "获取管理员消息列表")
    public Result<List<AdminNotificationItemVO>> getMessages() {
        return Result.success(notificationService.getAdminMessages());
    }

    /**
     * 创建管理员消息草稿或直接发布。
     *
     * @param requestDTO 消息内容和收件范围
     * @param principal 当前管理员
     * @return 创建结果
     */
    @PostMapping
    @Operation(description = "创建管理员消息")
    @AdminAudit(resourceType = AdminAuditResourceType.NOTIFICATION, action = AdminAuditAction.CREATE,
            resourceId = "#result.data.id")
    public Result<AdminNotificationItemVO> createMessage(
            @Valid @RequestBody CreateAdminNotificationRequestDTO requestDTO,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        return Result.success(notificationService.createAdminMessage(principal.userId(), requestDTO));
    }

    /**
     * 更新管理员消息草稿。
     *
     * @param notificationId 通知 ID
     * @param requestDTO 更新参数
     * @return 更新结果
     */
    @PutMapping("/{notificationId}")
    @Operation(description = "更新管理员消息草稿")
    @AdminAudit(resourceType = AdminAuditResourceType.NOTIFICATION, action = AdminAuditAction.UPDATE,
            resourceId = "#p0")
    public Result<AdminNotificationItemVO> updateDraft(
            @PathVariable @Positive Long notificationId,
            @Valid @RequestBody UpdateAdminNotificationRequestDTO requestDTO
    ) {
        return Result.success(notificationService.updateAdminMessage(notificationId, requestDTO));
    }

    /**
     * 发布或下线管理员消息。
     *
     * @param notificationId 通知 ID
     * @param status 目标状态
     * @return 更新结果
     */
    @PatchMapping("/{notificationId}/status")
    @Operation(description = "发布或下线管理员消息")
    @AdminAudit(resourceType = AdminAuditResourceType.NOTIFICATION, action = AdminAuditAction.CHANGE_STATUS,
            resourceId = "#p0", detail = "#p1")
    public Result<AdminNotificationItemVO> updateStatus(
            @PathVariable @Positive Long notificationId,
            @RequestParam String status
    ) {
        return Result.success(notificationService.updateAdminMessageStatus(notificationId, status));
    }
}
