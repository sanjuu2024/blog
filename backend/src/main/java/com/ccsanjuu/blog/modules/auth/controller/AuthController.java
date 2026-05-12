package com.ccsanjuu.blog.modules.auth.controller;

import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.modules.auth.support.RefreshTokenCookieManager;
import com.ccsanjuu.blog.modules.auth.model.dto.LoginRequestDTO;
import com.ccsanjuu.blog.modules.auth.model.dto.LogoutRequestDTO;
import com.ccsanjuu.blog.modules.auth.model.dto.RefreshTokenRequestDTO;
import com.ccsanjuu.blog.modules.auth.model.dto.RegisterRequestDTO;
import com.ccsanjuu.blog.modules.auth.model.vo.LoginVO;
import com.ccsanjuu.blog.modules.auth.model.vo.RefreshTokenVO;
import com.ccsanjuu.blog.modules.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Validated   // 需要参数校验
@Tag(name = "鉴权", description = "鉴权相关接口")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenCookieManager refreshTokenCookieManager;

    /**
     * 用户注册
     * @param registerRequestDTO
     * @return
     */
    @PostMapping("/register")
    @Operation(description = "注册")
    public Result<Void> register(@Valid @RequestBody RegisterRequestDTO registerRequestDTO) {
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
    // HttpServletResponse response：由 Spring MVC 的方法参数解析器自动传进来，代表当前这一次 HTTP 响应，可以用来设置响应头、状态码、Cookie 等。
    public Result<LoginVO> login(@Valid @RequestBody LoginRequestDTO loginRequestDTO,
                                 HttpServletResponse response
    ) {
        LoginVO loginVO = authService.login(loginRequestDTO);
        refreshTokenCookieManager.addRefreshTokenCookie(response, loginVO.getRefreshToken(), loginVO.getRefreshTokenExpiresAt());
        return Result.success(loginVO);
    }

    /**
     * 刷新登录态
     * @param refreshToken
     * @param response
     * @return
     */
    @PostMapping("/refresh")
    @Operation(description = "刷新登录态")
    public Result<RefreshTokenVO> refresh(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        RefreshTokenRequestDTO dto = RefreshTokenRequestDTO.builder()
                .refreshToken(refreshToken)
                .build();

        RefreshTokenVO refreshTokenVO = authService.refresh(dto);
        refreshTokenCookieManager.addRefreshTokenCookie(response, refreshTokenVO.getRefreshToken(), refreshTokenVO.getRefreshTokenExpiresAt());
        return Result.success(refreshTokenVO);
    }

    /**
     * 用户退出登录
     * @param refreshToken
     * @param response
     * @return
     */
    @PostMapping("/logout")
    @Operation(description = "用户退出登录")
    public Result<Void> logout(
            @CookieValue(name = "refresh_token", required = false) String refreshToken,
            HttpServletResponse response
    ) {
        LogoutRequestDTO dto = LogoutRequestDTO.builder()
                .refreshToken(refreshToken)
                .build();

        authService.logout(dto);
        refreshTokenCookieManager.clearRefreshTokenCookie(response);
        return Result.success(null);
    }
}
