package com.ccsanjuu.blog.modules.auth.service;

import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.modules.auth.model.dto.LoginRequestDTO;
import com.ccsanjuu.blog.modules.auth.model.dto.LogoutRequestDTO;
import com.ccsanjuu.blog.modules.auth.model.dto.RefreshTokenRequestDTO;
import com.ccsanjuu.blog.modules.auth.model.dto.RegisterRequestDTO;
import com.ccsanjuu.blog.modules.auth.model.vo.LoginVO;
import com.ccsanjuu.blog.modules.auth.model.vo.RefreshTokenVO;
import jakarta.validation.Valid;

public interface AuthService {
    /**
     * 用户注册
     * @param registerRequestDTO
     * @return
     */
    void register(RegisterRequestDTO registerRequestDTO);

    /**
     * 用户登录
     * @param loginRequestDTO
     * @return
     */
    LoginVO login(LoginRequestDTO loginRequestDTO);

    /**
     * 刷新登录态
     * @param refreshTokenRequestDTO
     * @return
     */
    RefreshTokenVO refresh(RefreshTokenRequestDTO refreshTokenRequestDTO);

    /**
     * 撤销旧的 RefreshToken
     * @param jti
     */
    void revokeRefreshToken(String jti);

    void revokeUserRefreshTokens(Long userId);

    /**
     * 用户退出登录
     * @param logoutRequestDTO
     */
    void logout(LogoutRequestDTO logoutRequestDTO);
}
