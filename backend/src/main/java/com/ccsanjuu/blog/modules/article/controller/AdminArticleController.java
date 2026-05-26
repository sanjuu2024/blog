package com.ccsanjuu.blog.modules.article.controller;

import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.modules.article.model.dto.AdminArticleQueryDTO;
import com.ccsanjuu.blog.modules.article.model.dto.ArticleUpsertRequestDTO;
import com.ccsanjuu.blog.modules.article.model.dto.UpdateArticleStatusRequestDTO;
import com.ccsanjuu.blog.modules.article.model.vo.*;
import com.ccsanjuu.blog.modules.article.service.ArticleService;
import com.ccsanjuu.blog.modules.auth.model.security.JwtPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/articles")
@Validated
@RequiredArgsConstructor
@Tag(name = "文章管理接口")
public class AdminArticleController {

    private final ArticleService articleService;

    /**
     * 获取后台文章分页列表
     *
     * @param adminArticleQueryDTO
     * @return
     */
    @GetMapping
    @Operation(description = "获取后台文章分页列表")
    public Result<PageResult<AdminArticleListItemVO>> getArticleList(@Valid @ModelAttribute AdminArticleQueryDTO adminArticleQueryDTO){
        return Result.success(articleService.getArticleList(adminArticleQueryDTO));
    }

    /**
     * 获取后台文章详情
     *
     * @param articleId
     * @return
     */
    @GetMapping("/{articleId}")
    @Operation(description = "获取后台文章详情")
    public Result<AdminArticleDetailVO> getArticleDetail(@Positive @PathVariable Long articleId){
        return Result.success(articleService.getArticleDetail(articleId));
    }

    /**
     * 创建文章
     *
     * @param articleUpsertRequestDTO
     * @param jwtPrincipal
     * @return
     */
    @PostMapping
    @Operation(description = "创建文章")
    public Result<CreatedArticleVO> createArticle(
            @Valid @RequestBody ArticleUpsertRequestDTO articleUpsertRequestDTO,
            @AuthenticationPrincipal JwtPrincipal jwtPrincipal
    ){
        return Result.success(articleService.createArticle(jwtPrincipal.userId(), articleUpsertRequestDTO));
    }

    /**
     * 更新文章
     *
     * @param articleId
     * @param articleUpsertRequestDTO
     * @param jwtPrincipal
     * @return
     */
    @PutMapping("/{articleId}")
    @Operation(description = "更新文章")
    public Result<UpdatedArticleVO> updateArticle(
            @Positive @PathVariable Long articleId,
            @Valid @RequestBody ArticleUpsertRequestDTO articleUpsertRequestDTO,
            @AuthenticationPrincipal JwtPrincipal jwtPrincipal
    ){
        return Result.success(articleService.updateArticle(articleId, jwtPrincipal.userId(), articleUpsertRequestDTO));
    }

    /**
     * 删除文章
     *
     * @param articleId
     * @param jwtPrincipal
     * @return
     */
    @DeleteMapping("/{articleId}")
    @Operation(description = "删除文章")
    public Result<Void> deleteArticle(
            @Positive @PathVariable Long articleId,
            @AuthenticationPrincipal JwtPrincipal jwtPrincipal
    ){
        articleService.deleteArticle(jwtPrincipal.userId(), articleId);
        return Result.success(null);
    }

    /**
     * 修改文章状态
     *
     * @param articleId
     * @param updateArticleStatusRequestDTO
     * @return
     */
    @PatchMapping("/{articleId}/status")
    @Operation(description = "修改文章状态")
    public Result<UpdatedArticleStatusVO> updateArticleStatus(
            @Positive @PathVariable Long articleId,
            @Valid @RequestBody UpdateArticleStatusRequestDTO updateArticleStatusRequestDTO
    ){
        return Result.success(articleService.updateArticleStatus(articleId, updateArticleStatusRequestDTO));
    }
}
