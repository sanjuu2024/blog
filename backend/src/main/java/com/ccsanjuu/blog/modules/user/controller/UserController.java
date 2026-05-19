package com.ccsanjuu.blog.modules.user.controller;

import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.modules.auth.model.security.JwtPrincipal;
import com.ccsanjuu.blog.modules.user.model.dto.ChangePasswordRequestDTO;
import com.ccsanjuu.blog.modules.user.model.dto.UpdateProfileRequestDTO;
import com.ccsanjuu.blog.modules.user.model.vo.CurrentUserProfileVO;
import com.ccsanjuu.blog.modules.user.model.vo.PublicUserProfileVO;
import com.ccsanjuu.blog.modules.user.model.vo.UpdatedUserProfileVO;
import com.ccsanjuu.blog.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@Validated
@Tag(name = "用户相关接口")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

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
