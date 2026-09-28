package com.ccsanjuu.blog.modules.notification.controller;

import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.modules.auth.model.security.JwtPrincipal;
import com.ccsanjuu.blog.modules.notification.model.dto.NotificationQueryDTO;
import com.ccsanjuu.blog.modules.notification.model.vo.NotificationItemVO;
import com.ccsanjuu.blog.modules.notification.model.vo.NotificationUnreadCountVO;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/notifications")
@Validated
@Tag(name = "站内通知接口")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 获取当前用户通知分页列表。
     *
     * @param queryDTO 查询条件
     * @param principal 当前用户
     * @return 通知分页结果
     */
    @GetMapping
    @Operation(description = "获取当前用户通知分页列表")
    public Result<PageResult<NotificationItemVO>> getNotifications(
            @Valid NotificationQueryDTO queryDTO,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        return Result.success(notificationService.getUserNotifications(principal.userId(), queryDTO));
    }

    /**
     * 获取当前用户未读数量。
     *
     * @param principal 当前用户
     * @return 未读数量
     */
    @GetMapping("/unread-count")
    @Operation(description = "获取当前用户通知未读数量")
    public Result<NotificationUnreadCountVO> getUnreadCount(@AuthenticationPrincipal JwtPrincipal principal) {
        return Result.success(notificationService.getUnreadCount(principal.userId()));
    }

    /**
     * 标记单条通知已读。
     *
     * @param notificationId 通知 ID
     * @param principal 当前用户
     * @return 无数据
     */
    @PatchMapping("/{notificationId}/read")
    @Operation(description = "标记单条通知已读")
    public Result<Void> markRead(
            @PathVariable @Positive Long notificationId,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        notificationService.markRead(principal.userId(), notificationId);
        return Result.success(null);
    }

    /**
     * 标记全部或指定分类通知已读。
     *
     * @param category 通知分类
     * @param principal 当前用户
     * @return 无数据
     */
    @PatchMapping("/read-all")
    @Operation(description = "标记全部或指定分类通知已读")
    public Result<Void> markAllRead(
            @RequestParam(required = false) String category,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        notificationService.markAllRead(principal.userId(), category);
        return Result.success(null);
    }
}
