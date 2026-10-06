package com.ccsanjuu.blog.modules.comment.controller;

import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.modules.auth.model.security.JwtPrincipal;
import com.ccsanjuu.blog.modules.comment.model.dto.CommentReplyQueryDTO;
import com.ccsanjuu.blog.modules.comment.model.vo.CommentDeleteVO;
import com.ccsanjuu.blog.modules.comment.model.vo.CommentLikeMutationVO;
import com.ccsanjuu.blog.modules.comment.model.vo.CommentReplyPageVO;
import com.ccsanjuu.blog.modules.comment.model.dto.CommentUnsubscribeRequestDTO;
import com.ccsanjuu.blog.modules.comment.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/comments")
@Validated   // 对于简单类型参数进行校验（搭配各自的校验注释使用）
@Tag(name = "公开评论接口")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    /**
     * 获取顶层评论下的回复
     *
     * @param commentId
     * @param dto
     * @param jwtPrincipal
     * @return
     */
    @GetMapping("/{commentId}/replies")
    @Operation(description = "获取顶层评论下的回复")
    public Result<CommentReplyPageVO> getRepliesByRootId(
            @PathVariable @Positive Long commentId,
            @ModelAttribute @Valid CommentReplyQueryDTO dto,
            @AuthenticationPrincipal JwtPrincipal jwtPrincipal
    ){
        Long currentUserId = jwtPrincipal == null ? null : jwtPrincipal.userId();
        return Result.success(commentService.getRepliesByRootId(commentId, currentUserId, dto));
    }

    /**
     * 删除自己的评论
     *
     * @param commentId
     * @param jwtPrincipal
     * @return
     */
    @DeleteMapping("/{commentId}")
    @Operation(description = "删除自己的评论")
    public Result<CommentDeleteVO> deleteOwnComment(
            @PathVariable @Positive Long commentId,
            @AuthenticationPrincipal JwtPrincipal jwtPrincipal
    ){
        return Result.success(commentService.deleteOwnComment(commentId, jwtPrincipal.userId()));
    }

    /**
     * 点赞评论。
     *
     * @param commentId 评论 ID
     * @param jwtPrincipal 当前登录用户
     * @return 点赞状态和最新点赞数
     */
    @PostMapping("/{commentId}/like")
    @Operation(description = "点赞评论")
    public Result<CommentLikeMutationVO> likeComment(
            @PathVariable @Positive Long commentId,
            @AuthenticationPrincipal JwtPrincipal jwtPrincipal
    ) {
        return Result.success(commentService.likeComment(commentId, jwtPrincipal.userId()));
    }

    /**
     * 取消评论点赞。
     *
     * @param commentId 评论 ID
     * @param jwtPrincipal 当前登录用户
     * @return 点赞状态和最新点赞数
     */
    @DeleteMapping("/{commentId}/like")
    @Operation(description = "取消评论点赞")
    public Result<CommentLikeMutationVO> unlikeComment(
            @PathVariable @Positive Long commentId,
            @AuthenticationPrincipal JwtPrincipal jwtPrincipal
    ) {
        return Result.success(commentService.unlikeComment(commentId, jwtPrincipal.userId()));
    }

    /**
     * 退订指定评论的直接回复邮件通知。
     *
     * @param token 评论退订令牌
     * @return 空响应
     */
    @PostMapping("/notifications/unsubscribe")
    @Operation(description = "退订评论直接回复通知")
    public Result<Void> unsubscribeReplyNotification(
            @RequestBody @Valid CommentUnsubscribeRequestDTO requestDTO
    ) {
        commentService.unsubscribeReplyNotification(requestDTO.getToken());
        return Result.success(null);
    }
}
