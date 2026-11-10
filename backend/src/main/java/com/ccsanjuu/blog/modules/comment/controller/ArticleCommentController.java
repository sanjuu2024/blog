package com.ccsanjuu.blog.modules.comment.controller;

import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.modules.auth.model.security.JwtPrincipal;
import com.ccsanjuu.blog.modules.comment.model.dto.CreateCommentRequestDTO;
import com.ccsanjuu.blog.modules.comment.model.dto.PublicCommentQueryDTO;
import com.ccsanjuu.blog.modules.comment.model.vo.CommentMutationVO;
import com.ccsanjuu.blog.modules.comment.model.vo.PublicCommentItemVO;
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
@RequestMapping("/articles/{articleId}/comments")
@Validated   // 对于简单类型参数进行校验（搭配各自的校验注释使用）
@Tag(name = "公开评论接口")
@RequiredArgsConstructor
public class ArticleCommentController {

    private final CommentService commentService;

    /**
     * 获取文章顶层评论分页列表
     *
     * @param articleId
     * @param dto
     * @param jwtPrincipal
     * @return
     */
    @GetMapping
    @Operation(description = "获取文章顶层评论分页列表")
    public Result<PageResult<PublicCommentItemVO>> getPublicCommentList(
            @PathVariable @Positive Long articleId,
            @ModelAttribute @Valid PublicCommentQueryDTO dto,
            @AuthenticationPrincipal JwtPrincipal jwtPrincipal
    ){
        Long currentUserId = jwtPrincipal == null ? null : jwtPrincipal.userId();
        return Result.success(commentService.getPublicCommentList(articleId, currentUserId, dto));
    }

    /**
     * 发表评论或回复
     *
     * @param articleId
     * @param dto
     * @param jwtPrincipal
     * @return
     */
    @PostMapping
    @Operation(description = "发表评论或回复")
    public Result<CommentMutationVO> createComment(
            @PathVariable @Positive Long articleId,
            @RequestBody @Valid CreateCommentRequestDTO dto,
            @AuthenticationPrincipal JwtPrincipal jwtPrincipal
    ){
        return Result.success(commentService.createComment(articleId, jwtPrincipal.userId(), dto));
    }
}
