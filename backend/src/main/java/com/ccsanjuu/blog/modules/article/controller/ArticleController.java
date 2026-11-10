package com.ccsanjuu.blog.modules.article.controller;

import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.modules.article.model.bo.ArticleViewIdentity;
import com.ccsanjuu.blog.modules.article.model.bo.ArticleLikeIdentity;
import com.ccsanjuu.blog.modules.article.model.dto.PublicArticleQueryDTO;
import com.ccsanjuu.blog.modules.article.model.vo.PublicArticleDetailVO;
import com.ccsanjuu.blog.modules.article.model.vo.PublicArticleListItemVO;
import com.ccsanjuu.blog.modules.article.model.vo.ArticleLikeMutationVO;
import com.ccsanjuu.blog.modules.auth.model.security.JwtPrincipal;
import com.ccsanjuu.blog.modules.article.service.ArticleService;
import com.ccsanjuu.blog.modules.article.service.ArticleLikeService;
import com.ccsanjuu.blog.modules.article.support.VisitorIdCookieManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/articles")
@RequiredArgsConstructor
@Tag(name = "前台文章接口")
@Validated
public class ArticleController {

    private final ArticleService articleService;
    private final ArticleLikeService articleLikeService;
    private final VisitorIdCookieManager visitorIdCookieManager;

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
    public Result<PublicArticleDetailVO> getPublicArticleDetail(
            @Positive @PathVariable Long articleId,
            @AuthenticationPrincipal JwtPrincipal principal,
            HttpServletRequest request,
            HttpServletResponse response
    ){
        String visitorToken = principal == null
                ? visitorIdCookieManager.resolve(request, response)
                : null;
        ArticleViewIdentity viewIdentity = principal == null
                ? new ArticleViewIdentity(null, visitorToken)
                : new ArticleViewIdentity(principal.userId(), null);
        ArticleLikeIdentity likeIdentity = principal == null
                ? new ArticleLikeIdentity(null, visitorToken)
                : new ArticleLikeIdentity(principal.userId(), null);
        return Result.success(articleService.getPublicArticleDetail(articleId, viewIdentity, likeIdentity));
    }

    /**
     * 为文章增加当前主体的点赞。
     *
     * @param articleId 文章 ID
     * @param principal 当前登录用户；游客请求时为空
     * @param request 当前 HTTP 请求
     * @param response 用于首次访问时设置游客身份 Cookie
     * @return 点赞状态和最新点赞数
     */
    @PostMapping("/{articleId}/like")
    @Operation(description = "为文章点赞")
    public Result<ArticleLikeMutationVO> likeArticle(
            @Positive @PathVariable Long articleId,
            @AuthenticationPrincipal JwtPrincipal principal,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return Result.success(articleLikeService.likeArticle(
                articleId,
                resolveLikeIdentity(principal, request, response)
        ));
    }

    /**
     * 取消当前主体的文章点赞。
     *
     * @param articleId 文章 ID
     * @param principal 当前登录用户；游客请求时为空
     * @param request 当前 HTTP 请求
     * @param response 用于首次访问时设置游客身份 Cookie
     * @return 点赞状态和最新点赞数
     */
    @DeleteMapping("/{articleId}/like")
    @Operation(description = "取消文章点赞")
    public Result<ArticleLikeMutationVO> unlikeArticle(
            @Positive @PathVariable Long articleId,
            @AuthenticationPrincipal JwtPrincipal principal,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return Result.success(articleLikeService.unlikeArticle(
                articleId,
                resolveLikeIdentity(principal, request, response)
        ));
    }

    /**
     * 解析文章点赞接口使用的登录用户或游客身份。
     *
     * @param principal 当前登录用户；游客请求时为空
     * @param request 当前 HTTP 请求
     * @param response 用于首次访问时设置游客身份 Cookie
     * @return 点赞主体身份
     */
    private ArticleLikeIdentity resolveLikeIdentity(
            JwtPrincipal principal,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return principal == null
                ? new ArticleLikeIdentity(null, visitorIdCookieManager.resolve(request, response))
                : new ArticleLikeIdentity(principal.userId(), null);
    }
}
