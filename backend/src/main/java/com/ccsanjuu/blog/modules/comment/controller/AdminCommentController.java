package com.ccsanjuu.blog.modules.comment.controller;

import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.modules.auth.model.security.JwtPrincipal;
import com.ccsanjuu.blog.modules.comment.model.dto.AdminCommentQueryDTO;
import com.ccsanjuu.blog.modules.comment.model.dto.CommentModerationRequestDTO;
import com.ccsanjuu.blog.modules.comment.model.vo.AdminCommentItemVO;
import com.ccsanjuu.blog.modules.comment.model.vo.CommentMutationVO;
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
@RequestMapping("/admin/comments")
@Validated   // 对于简单类型参数进行校验（搭配各自的校验注释使用）
@Tag(name = "评论管理接口")
@RequiredArgsConstructor
public class AdminCommentController {

    private final CommentService commentService;

    /**
     * 获取后台评论分页列表
     * @param adminCommentQueryDTO
     * @return
     */
    @GetMapping
    @Operation(description = "获取后台评论分页列表")
    public Result<PageResult<AdminCommentItemVO>> getAdminCommentList(@Valid @ModelAttribute AdminCommentQueryDTO adminCommentQueryDTO){
        return Result.success(commentService.getAdminCommentList(adminCommentQueryDTO));
    }

    /**
     * 审核、隐藏或删除评论
     * @param commentId
     * @param commentModerationRequestDTO
     * @return
     */
    @PatchMapping("/{commentId}/moderation")
    @Operation(description = "审核、隐藏或删除评论")
    public Result<CommentMutationVO> moderateComment(
            @PathVariable @Positive Long commentId,
            @Valid @RequestBody CommentModerationRequestDTO commentModerationRequestDTO,
            @AuthenticationPrincipal JwtPrincipal jwtPrincipal
    ){
        return Result.success(commentService.moderateComment(commentId, jwtPrincipal.userId(), commentModerationRequestDTO));
    }
}
