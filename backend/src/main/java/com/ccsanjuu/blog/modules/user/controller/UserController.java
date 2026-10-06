package com.ccsanjuu.blog.modules.user.controller;

import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.common.api.PageQuery;
import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.modules.article.service.ArticleService;
import com.ccsanjuu.blog.modules.article.model.vo.PublicArticleListItemVO;
import com.ccsanjuu.blog.modules.auth.model.security.JwtPrincipal;
import com.ccsanjuu.blog.modules.user.model.dto.ChangePasswordRequestDTO;
import com.ccsanjuu.blog.modules.user.model.dto.ChangeEmailRequestDTO;
import com.ccsanjuu.blog.modules.user.model.dto.SendEmailChangeCodeRequestDTO;
import com.ccsanjuu.blog.modules.user.model.dto.UpdateProfileRequestDTO;
import com.ccsanjuu.blog.modules.user.model.vo.CurrentUserProfileVO;
import com.ccsanjuu.blog.modules.user.model.vo.PublicUserProfileVO;
import com.ccsanjuu.blog.modules.user.model.vo.UpdatedUserProfileVO;
import com.ccsanjuu.blog.modules.user.model.vo.UpdatedUserAvatarVO;
import com.ccsanjuu.blog.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users")
@Validated
@Tag(name = "用户相关接口")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final ArticleService articleService;

    /**
     * 获取用户公开资料卡
     * @param userId
     * @return
     */
    @GetMapping("/{userId}/public-profile")
    @Operation(description = "获取用户公开资料卡")
    public Result<PublicUserProfileVO> getPublicUserProfile(@Positive @PathVariable Long userId) {
        return Result.success(userService.getPublicUserProfile(userId));
    }

    /**
     * 获取当前登录用户信息
     * @param jwtPrincipal
     * @return
     */
    @GetMapping("/me")
    @Operation(description = "获取当前登录用户信息")
    public Result<CurrentUserProfileVO> getCurrentUserProfile (
            @AuthenticationPrincipal JwtPrincipal jwtPrincipal   // 🔺🔺🔺获取 jwt 拦截器拦截解析 token 后放入的用户信息，注意是作为方法的参数传递进来的
    ){
        return Result.success(userService.getCurrentUserProfile(jwtPrincipal.userId()));
    }

    /**
     * 获取当前用户点赞过的文章。
     *
     * @param pageQuery 分页参数
     * @param jwtPrincipal 当前登录用户
     * @return 点赞文章分页结果
     */
    @GetMapping("/me/liked-articles")
    @Operation(description = "获取当前用户点赞过的文章")
    public Result<PageResult<PublicArticleListItemVO>> getLikedArticles(
            @Valid @ModelAttribute PageQuery pageQuery,
            @AuthenticationPrincipal JwtPrincipal jwtPrincipal
    ) {
        return Result.success(articleService.getLikedArticleList(jwtPrincipal.userId(), pageQuery));
    }

    /**
     * 更新个人资料
     * @param updateProfileRequestDTO
     * @param jwtPrincipal
     * @return
     */
    @PutMapping("/me/profile")
    @Operation(description = "更新个人资料")
    public Result<UpdatedUserProfileVO> updateProfile(
            @Valid @RequestBody UpdateProfileRequestDTO updateProfileRequestDTO,
            @AuthenticationPrincipal JwtPrincipal jwtPrincipal   // 🔺🔺🔺获取 jwt 拦截器拦截解析 token 后放入的用户信息，注意是作为方法的参数传递进来的
    ){
        return Result.success(userService.updateProfile(jwtPrincipal.userId(), updateProfileRequestDTO));
    }

    /**
     * 发送修改邮箱验证码。
     *
     * @param requestDTO 新邮箱参数
     * @param jwtPrincipal 当前登录用户
     * @param request 当前 HTTP 请求
     * @return 空响应
     */
    @PostMapping("/me/email-verification-codes")
    @Operation(description = "发送修改邮箱验证码")
    public Result<Void> sendEmailChangeCode(
            @Valid @RequestBody SendEmailChangeCodeRequestDTO requestDTO,
            @AuthenticationPrincipal JwtPrincipal jwtPrincipal,
            HttpServletRequest request
    ) {
        userService.sendEmailChangeCode(jwtPrincipal.userId(), requestDTO.getEmail(), request.getRemoteAddr());
        return Result.success(null);
    }

    /**
     * 修改当前用户邮箱。
     *
     * @param requestDTO 修改邮箱参数
     * @param jwtPrincipal 当前登录用户
     * @return 更新后的个人资料
     */
    @PutMapping("/me/email")
    @Operation(description = "修改当前用户邮箱")
    public Result<UpdatedUserProfileVO> changeEmail(
            @Valid @RequestBody ChangeEmailRequestDTO requestDTO,
            @AuthenticationPrincipal JwtPrincipal jwtPrincipal
    ) {
        return Result.success(userService.changeEmail(jwtPrincipal.userId(), requestDTO));
    }

    /**
     * 上传并更新当前用户头像。
     *
     * @param file 头像图片
     * @param jwtPrincipal 当前登录用户
     * @return 更新后的头像信息
     */
    @PutMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(description = "上传并更新当前用户头像")
    public Result<UpdatedUserAvatarVO> updateAvatar(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal JwtPrincipal jwtPrincipal
    ) {
        return Result.success(userService.updateAvatar(jwtPrincipal.userId(), file));
    }

    /**
     * 修改密码
     * @param changePasswordRequestDTO
     * @param jwtPrincipal
     * @return
     */
    @PutMapping("/me/password")
    @Operation(description = "修改密码")
    public Result<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequestDTO changePasswordRequestDTO,
            @AuthenticationPrincipal JwtPrincipal jwtPrincipal
    ){
        return Result.success(userService.changePassword(jwtPrincipal.userId(), changePasswordRequestDTO));
    }
}
