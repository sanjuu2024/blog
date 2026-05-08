package com.ccsanjuu.blog.modules.auth.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.common.util.JwtUtil;
import com.ccsanjuu.blog.common.util.TokenHashUtil;
import com.ccsanjuu.blog.modules.auth.constants.AuthConstants;
import com.ccsanjuu.blog.modules.auth.mapper.AuthMapper;
import com.ccsanjuu.blog.modules.auth.model.bo.AuthTokenPair;
import com.ccsanjuu.blog.modules.auth.model.bo.ValidatedRefreshToken;
import com.ccsanjuu.blog.modules.auth.model.dto.LoginRequestDTO;
import com.ccsanjuu.blog.modules.auth.model.dto.LogoutRequestDTO;
import com.ccsanjuu.blog.modules.auth.model.dto.RefreshTokenRequestDTO;
import com.ccsanjuu.blog.modules.auth.model.dto.RegisterRequestDTO;
import com.ccsanjuu.blog.modules.auth.model.entity.AuthSession;
import com.ccsanjuu.blog.modules.auth.model.enums.AuthSessionStatus;
import com.ccsanjuu.blog.modules.auth.model.enums.AuthSessionTokenType;
import com.ccsanjuu.blog.modules.auth.model.vo.LoginUserVO;
import com.ccsanjuu.blog.modules.auth.model.vo.LoginVO;
import com.ccsanjuu.blog.modules.auth.model.vo.RefreshTokenVO;
import com.ccsanjuu.blog.modules.auth.service.AuthService;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import com.ccsanjuu.blog.modules.user.model.enums.UserRole;
import com.ccsanjuu.blog.modules.user.model.enums.UserStatus;
import com.ccsanjuu.blog.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;
    private final SecretKey jwtSigningKey;
    private final AuthMapper authMapper;

    /**
     * 用户注册
     * @param registerRequestDTO
     */
    @Override
    public void register(RegisterRequestDTO registerRequestDTO) {
        // 1. 验证用户名唯一性
        User user = findUserByUsername(registerRequestDTO.getUsername());
        if (user != null) {
            throw new BizException(ResultCode.USERNAME_EXISTS);
        }

        // 2. 验证邮箱唯一性
        user = findUserByEmail(registerRequestDTO.getEmail());
        if (user != null) {
            throw new BizException(ResultCode.EMAIL_EXISTS);
        }

        // 3. 密码加密，封装要插入的 User 对象
        User newUser = User.builder()
                .username(registerRequestDTO.getUsername())
                .nickname(registerRequestDTO.getUsername())  // 初始昵称默认同用户名
                .email(registerRequestDTO.getEmail())
                .passwordHash(passwordEncoder.encode(registerRequestDTO.getPassword()))
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .avatarUrl("")
                .build();

        // 4，插入用户信息
        userMapper.insert(newUser);
    }

    /**
     * 用户登录
     * @param loginRequestDTO
     * @return
     */
    @Override
    public LoginVO login(LoginRequestDTO loginRequestDTO) {
        // 🍰1. 根据 account 是否包含 @，分别按邮箱或用户名查询用户
        User user = null;
        if (loginRequestDTO.getAccount().contains("@")){
            user = findUserByEmail(loginRequestDTO.getAccount());
        }
        else{
            user = findUserByUsername(loginRequestDTO.getAccount());
        }
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }

        // 🍰2. 密码是否正确
        if (!passwordEncoder.matches(loginRequestDTO.getPassword(), user.getPasswordHash())) {
            throw new BizException(ResultCode.PASSWORD_ERROR);
        }

        // 🍰3. 生成 AccessToken 和 RefreshToken 以及各自的过期时间，并将 RefreshToken 存入数据库
        AuthTokenPair authTokenPair = generateTokenPairAndSaveRT(user);

        // 🍰4. 封装返回
        LoginUserVO loginUserVO = BeanUtil.copyProperties(user, LoginUserVO.class);
        return LoginVO.builder()
                .accessToken(authTokenPair.getAccessToken())
                .accessTokenExpiresAt(authTokenPair.getAccessTokenExpiresAt())
                .refreshToken(authTokenPair.getRefreshToken())
                .refreshTokenExpiresAt(authTokenPair.getRefreshTokenExpiresAt())
                .tokenType(AuthConstants.BEARER_TOKEN_TYPE)
                .user(loginUserVO)
                .build();
    }

    /**
     * 刷新登录态
     * @param refreshTokenRequestDTO
     * @return
     */
    @Override
    @Transactional
    public RefreshTokenVO refresh(RefreshTokenRequestDTO refreshTokenRequestDTO) {
        // 🍰1. 解析并验证 DTO 中的旧 RT 是否有效（包括判断token_type、是否存在、是否ACTIVE可用、是否没过期、是否和数据库中的token_hash相等）
        String refreshToken = refreshTokenRequestDTO.getRefreshToken();
        ValidatedRefreshToken validatedRefreshToken = validateRefreshToken(refreshToken);

        // 🍰2. 检查当前用户是否存在、是否没被禁用
        User user = userMapper.selectById(validatedRefreshToken.getUserId());
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        if (user.getStatus() == UserStatus.DISABLED){
            throw new BizException(ResultCode.USER_DISABLED);
        }

        // 🍰3. 撤销旧 RT
        revokeRefreshToken(validatedRefreshToken.getTokenJti());

        // 🍰4. 颁发新的 RT 和 AT，并且将新 RT 存入数据库
        AuthTokenPair authTokenPair = generateTokenPairAndSaveRT(user);

        // 🍰5. 封装返回
        return RefreshTokenVO.builder()
                .accessToken(authTokenPair.getAccessToken())
                .accessTokenExpiresAt(authTokenPair.getAccessTokenExpiresAt())
                .refreshToken(authTokenPair.getRefreshToken())
                .refreshTokenExpiresAt(authTokenPair.getRefreshTokenExpiresAt())
                .tokenType(AuthConstants.BEARER_TOKEN_TYPE)
                .build();
    }

    /**
     * 用户退出登录
     * @param logoutRequestDTO
     */
    @Override
    @Transactional
    public void logout(LogoutRequestDTO logoutRequestDTO) {
        if (logoutRequestDTO.getRefreshToken() == null || logoutRequestDTO.getRefreshToken().isBlank()) {
            return;
        }

        try {
            ValidatedRefreshToken validatedRefreshToken = validateRefreshToken(logoutRequestDTO.getRefreshToken());
            revokeRefreshToken(validatedRefreshToken.getTokenJti());
        } catch (BizException ex) {
            if (ex.getResultCode() != ResultCode.REFRESH_TOKEN_INVALID_OR_EXPIRED) {
                throw ex;
            }
            // 退出登录按幂等语义处理：RT 无效、过期、已撤销或找不到会话，都视为已经退出。
            log.debug("用户退出登录成功，忽略发生的异常: {}", ex.getMessage());
        }
    }

    /**
     * 根据 username 查找用户
     * @param username
     * @return
     */
    private User findUserByUsername(String username) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .apply("LOWER(username) = LOWER({0})", username)
        );
    }

    /**
     * 根据 email 查找用户
     * @param email
     * @return
     */
    private User findUserByEmail(String email) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .apply("LOWER(email) = LOWER({0})", email)

        );
    }

    /**
     * 撤销旧的 RefreshToken
     * @param jti
     */
    @Override
    public void revokeRefreshToken(String jti) {
        authMapper.revokeRefreshToken(jti, AuthSessionStatus.REVOKED.getValue());
    }

    /**
     * 生成 AT、RT 以及各自的过期时间
     * @return
     */
    private AuthTokenPair generateTokenPair(User user){
        String refreshTokenJti = IdUtil.fastSimpleUUID();

        String accessToken = JwtUtil.generateAccessToken(
                jwtSigningKey,
                jwtProperties.issuer(),
                jwtProperties.accessTtl(),
                user.getId(),
                user.getUsername(),
                user.getRole().getValue(),
                user.getStatus().getValue()
        );

        String refreshToken = JwtUtil.generateRefreshToken(
                jwtSigningKey,
                jwtProperties.issuer(),
                jwtProperties.refreshTtl(),
                user.getId(),
                refreshTokenJti
        );

        // 🍰2. 生成过期时间
        OffsetDateTime accessTokenExpiresAt = OffsetDateTime.ofInstant(
                JwtUtil.getExpiration(accessToken, jwtSigningKey),
                ZoneOffset.UTC
        );

        OffsetDateTime refreshTokenExpiresAt = OffsetDateTime.ofInstant(
                JwtUtil.getExpiration(refreshToken, jwtSigningKey),
                ZoneOffset.UTC
        );

        // 🍰3. 封装返回
        return AuthTokenPair.builder()
                .accessToken(accessToken)
                .accessTokenExpiresAt(accessTokenExpiresAt)
                .refreshToken(refreshToken)
                .refreshTokenExpiresAt(refreshTokenExpiresAt)
                .refreshTokenJti(refreshTokenJti)
                .build();
    }

    /**
     * 生成 AT、RT 以及各自的过期时间，并将 RefreshToken 存入数据库
     * @return
     */
    private AuthTokenPair generateTokenPairAndSaveRT(User user){
        // 🍰1. 获取 authTokenPair
        AuthTokenPair authTokenPair = generateTokenPair(user);

        // 🍰2. 将 RefreshToken 存入数据库
        authMapper.insert(AuthSession.builder()
                .userId(user.getId())
                .tokenJti(authTokenPair.getRefreshTokenJti())
                .tokenHash(TokenHashUtil.sha256(authTokenPair.getRefreshToken()))
                .tokenType(AuthSessionTokenType.REFRESH)
                .status(AuthSessionStatus.ACTIVE)
                .expiresAt(authTokenPair.getRefreshTokenExpiresAt())
                .build()
        );

        // 🍰3. 返回 authTokenPair
        return authTokenPair;
    }

    /**
     * 解析并验证 RT 是否有效（包括判断token_type、是否存在、是否ACTIVE可用、是否没过期、是否和数据库中的token_hash相等）
     * @param refreshToken
     * @return
     */
    private ValidatedRefreshToken validateRefreshToken(String refreshToken){
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BizException(ResultCode.REFRESH_TOKEN_INVALID_OR_EXPIRED);
        }

        String jti = null;
        Long userId = null;
        AuthSession authSession = null;
        try{
            Claims claims = JwtUtil.parseClaims(refreshToken, jwtSigningKey);
            if (!JwtUtil.TOKEN_TYPE_REFRESH.equals(claims.get(JwtUtil.CLAIM_TOKEN_TYPE, String.class))) {
                throw new BizException(ResultCode.REFRESH_TOKEN_INVALID_OR_EXPIRED);   // 携带的 token 类型错误
            }

            jti = claims.getId();
            if (jti == null || jti.isBlank()) {
                throw new BizException(ResultCode.REFRESH_TOKEN_INVALID_OR_EXPIRED);   // jti错误
            }

            userId = JwtUtil.getUserId(claims);
            authSession = authMapper.selectOne(
                    new LambdaQueryWrapper<AuthSession>()
                            .eq(AuthSession::getTokenJti, jti)
                            .eq(AuthSession::getTokenType, AuthSessionTokenType.REFRESH)
                            .eq(AuthSession::getStatus, AuthSessionStatus.ACTIVE)
                            .eq(AuthSession::getUserId, userId)
            );
            if (authSession == null) {
                throw new BizException(ResultCode.REFRESH_TOKEN_INVALID_OR_EXPIRED);
            }

            if (authSession.getExpiresAt() == null
                    || !authSession.getExpiresAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC))) {
                throw new BizException(ResultCode.REFRESH_TOKEN_INVALID_OR_EXPIRED);
            }

            if (!authSession.getTokenHash().equals(TokenHashUtil.sha256(refreshToken))) {
                throw new BizException(ResultCode.REFRESH_TOKEN_INVALID_OR_EXPIRED);   // 旧 rt 值错误
            }
        } catch (JwtException | IllegalArgumentException ex) {
            throw new BizException(ResultCode.REFRESH_TOKEN_INVALID_OR_EXPIRED);   // 旧 rt 值错误
        }

        return ValidatedRefreshToken.builder()
                .refreshToken(refreshToken)
                .userId(userId)
                .tokenJti(jti)
                .authSession(authSession)
                .build();
    }
}
