package com.ccsanjuu.blog.modules.message.controller;

import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.modules.auth.model.security.JwtPrincipal;
import com.ccsanjuu.blog.modules.message.model.dto.CreateMessageReplyRequestDTO;
import com.ccsanjuu.blog.modules.message.model.dto.MessageBatchApprovalRequestDTO;
import com.ccsanjuu.blog.modules.message.model.dto.MessageModerationRequestDTO;
import com.ccsanjuu.blog.modules.message.model.dto.MessagePageQueryDTO;
import com.ccsanjuu.blog.modules.message.model.vo.AdminMessageItemVO;
import com.ccsanjuu.blog.modules.message.model.vo.MessageMutationVO;
import com.ccsanjuu.blog.modules.message.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/messages")
@Validated
@Tag(name = "留言管理接口")
@RequiredArgsConstructor
public class AdminMessageController {

    private final MessageService messageService;

    /**
     * 获取后台留言分页列表。
     *
     * @param queryDTO 查询条件
     * @return 后台留言分页结果
     */
    @GetMapping
    @Operation(description = "获取后台留言分页列表")
    public Result<PageResult<AdminMessageItemVO>> getAdminMessageList(@Valid @ModelAttribute MessagePageQueryDTO queryDTO) {
        return Result.success(messageService.getAdminMessageList(queryDTO));
    }

    /**
     * 审核、隐藏或删除留言。
     *
     * @param messageId 留言 ID
     * @param requestDTO 审核参数
     * @param principal 当前管理员
     * @return 处理后的留言
     */
    @PatchMapping("/{messageId}/moderation")
    @Operation(description = "审核、隐藏或删除留言")
    public Result<MessageMutationVO> moderateMessage(
            @PathVariable @Positive Long messageId,
            @Valid @RequestBody MessageModerationRequestDTO requestDTO,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        return Result.success(messageService.moderateMessage(messageId, principal.userId(), requestDTO));
    }

    /**
     * 管理员回复已通过的顶层留言。
     *
     * @param messageId 顶层留言 ID
     * @param requestDTO 回复参数
     * @param principal 当前管理员
     * @return 回复结果
     */
    @PostMapping("/{messageId}/replies")
    @Operation(description = "管理员回复留言")
    public Result<MessageMutationVO> replyMessage(
            @PathVariable @Positive Long messageId,
            @Valid @RequestBody CreateMessageReplyRequestDTO requestDTO,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        return Result.success(messageService.replyMessage(messageId, principal.userId(), requestDTO));
    }

    /**
     * 批量通过待审核顶层留言。
     *
     * @param requestDTO 留言 ID 列表
     * @param principal 当前管理员
     * @return 无数据
     */
    @PatchMapping("/batch-approval")
    @Operation(description = "批量通过待审核顶层留言")
    public Result<Void> approveMessages(
            @Valid @RequestBody MessageBatchApprovalRequestDTO requestDTO,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        messageService.approveMessages(principal.userId(), requestDTO);
        return Result.success(null);
    }
}
