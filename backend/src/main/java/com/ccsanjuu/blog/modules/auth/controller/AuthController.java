package com.ccsanjuu.blog.modules.auth.controller;

import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.modules.auth.model.dto.LoginRequestDTO;
import com.ccsanjuu.blog.modules.auth.model.dto.LogoutRequestDTO;
import com.ccsanjuu.blog.modules.auth.model.dto.RefreshTokenRequestDTO;
import com.ccsanjuu.blog.modules.auth.model.dto.RegisterRequestDTO;
import com.ccsanjuu.blog.modules.auth.model.vo.LoginVO;
import com.ccsanjuu.blog.modules.auth.model.vo.RefreshTokenVO;
import com.ccsanjuu.blog.modules.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@Validated   // 需要参数校验
@Tag(name = "鉴权", description = "鉴权相关接口")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;

    /**
     * 用户注册
     * @param registerRequestDTO
     * @return
     */
    @PostMapping("/register")
    @Operation(description = "注册")
    public Result<Void> register(@Valid @RequestBody RegisterRequestDTO registerRequestDTO) {
        log.info("注册请求参数：{}", registerRequestDTO);
        authService.register(registerRequestDTO);
        return Result.success(null);
    }

    /**
     * 用户登录
     * @param loginRequestDTO
     * @return
     */
    @PostMapping("/login")
    @Operation(description = "登录")
    public Result<LoginVO> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
        log.info("登录请求参数：{}", loginRequestDTO);
        return Result.success(authService.login(loginRequestDTO));
    }

    /**
     * 刷新登录态
     * @param refreshTokenRequestDTO
     * @return
     */
    @PostMapping("/refresh")
    @Operation(description = "刷新登录态")
    public Result<RefreshTokenVO> refresh(@Valid @RequestBody RefreshTokenRequestDTO refreshTokenRequestDTO) {
        log.info("刷新登录态请求参数：{}", refreshTokenRequestDTO);
        return Result.success(authService.refresh(refreshTokenRequestDTO));
    }

    /**
     * 用户退出登录
     * @param logoutRequestDTO
     * @return
     */
    @PostMapping("/logout")
    @Operation(description = "用户退出登录")
    public Result<Map<String, Boolean>> logout(@Valid @RequestBody LogoutRequestDTO logoutRequestDTO){
        log.info("用户退出登录请求参数：{}",logoutRequestDTO);
        authService.logout(logoutRequestDTO);
        return Result.success(Map.of("success", true));
    }
}
