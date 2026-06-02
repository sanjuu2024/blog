package com.ccsanjuu.blog.modules.article.controller;

import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.modules.article.model.dto.PublicArticleQueryDTO;
import com.ccsanjuu.blog.modules.article.model.vo.PublicArticleDetailVO;
import com.ccsanjuu.blog.modules.article.model.vo.PublicArticleListItemVO;
import com.ccsanjuu.blog.modules.article.service.ArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/articles")
@RequiredArgsConstructor
@Tag(name = "前台文章接口")
@Validated
public class ArticleController {

    private final ArticleService articleService;

    /**
     * 获取已发布文章分页列表
     *
     * @param queryDTO
     * @return
     */
    @GetMapping
    @Operation(description = "获取已发布文章分页列表")
    public Result<PageResult<PublicArticleListItemVO>> getPublicArticleList(@Valid @ModelAttribute PublicArticleQueryDTO queryDTO){
        return Result.success(articleService.getPublicArticleList(queryDTO));
    }

    /**
     * 获取前台文章详情
     *
     * @param articleId
     * @return
     */
    @GetMapping("/{articleId}")
    @Operation(description = "获取前台文章详情")
    public Result<PublicArticleDetailVO> getPublicArticleDetail(@Positive @PathVariable Long articleId){
        return Result.success(articleService.getPublicArticleDetail(articleId));
    }
}
