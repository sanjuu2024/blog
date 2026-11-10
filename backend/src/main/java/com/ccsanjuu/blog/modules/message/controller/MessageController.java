package com.ccsanjuu.blog.modules.message.controller;

import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.modules.auth.model.security.JwtPrincipal;
import com.ccsanjuu.blog.modules.message.model.dto.CreateMessageRequestDTO;
import com.ccsanjuu.blog.modules.message.model.dto.MessagePageQueryDTO;
import com.ccsanjuu.blog.modules.message.model.dto.MessageUnsubscribeRequestDTO;
import com.ccsanjuu.blog.modules.message.model.vo.MessageMutationVO;
import com.ccsanjuu.blog.modules.message.model.vo.PublicMessageItemVO;
import com.ccsanjuu.blog.modules.message.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/messages")
@Validated
@Tag(name = "留言接口")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    /**
     * 获取公开留言分页列表。
     *
     * @param queryDTO 分页参数
     * @param principal 可选登录用户
     * @return 留言分页结果
     */
    @GetMapping
    @Operation(description = "获取公开留言分页列表")
    public Result<PageResult<PublicMessageItemVO>> getPublicMessageList(
            @Valid @ModelAttribute MessagePageQueryDTO queryDTO,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        Long userId = principal == null ? null : principal.userId();
        return Result.success(messageService.getPublicMessageList(userId, queryDTO));
    }

    /**
     * 创建游客或登录用户的顶层留言。
     *
     * @param requestDTO 创建参数
     * @param principal 可选登录用户
     * @param request HTTP 请求
     * @return 创建结果
     */
    @PostMapping
    @Operation(description = "发表留言")
    public Result<MessageMutationVO> createMessage(
            @Valid @RequestBody CreateMessageRequestDTO requestDTO,
            @AuthenticationPrincipal JwtPrincipal principal,
            HttpServletRequest request
    ) {
        Long userId = principal == null ? null : principal.userId();
        return Result.success(messageService.createMessage(userId, request.getRemoteAddr(), requestDTO));
    }

    /**
     * 删除当前登录用户自己的顶层留言。
     *
     * @param messageId 留言 ID
     * @param principal 当前登录用户
     * @return 无数据
     */
    @DeleteMapping("/{messageId}")
    @Operation(description = "删除自己的留言")
    public Result<Void> deleteOwnMessage(
            @PathVariable @Positive Long messageId,
            @AuthenticationPrincipal JwtPrincipal principal
    ) {
        messageService.deleteOwnMessage(messageId, principal.userId());
        return Result.success(null);
    }

    /**
     * 关闭单条顶层留言的后续回复通知。
     *
     * @param requestDTO 退订令牌
     * @return 无数据
     */
    @PostMapping("/notifications/unsubscribe")
    @Operation(description = "退订留言回复通知")
    public Result<Void> unsubscribe(@Valid @RequestBody MessageUnsubscribeRequestDTO requestDTO) {
        messageService.unsubscribe(requestDTO);
        return Result.success(null);
    }
}
