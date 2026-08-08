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
                .tokenVersion(0L)
                .avatarUrl("")
                .build();

        // 4，插入用户信息
        userMapper.insert(newUser);

        log.info(
                "security_event=REGISTER_SUCCESS description=\"用户注册成功\" outcome=SUCCESS userId={} username={}",
                newUser.getId(),
                newUser.getUsername()
        );
    }

    /**
     * 用户登录
     * @param loginRequestDTO
     * @return
     */
    @Override
    @Transactional
    public LoginVO login(LoginRequestDTO loginRequestDTO) {
        // 🍰1. 根据 account 是否包含 @，分别按邮箱或用户名查询用户
        User user = null;
        if (loginRequestDTO.getAccount().contains("@")){
            user = findUserByEmail(loginRequestDTO.getAccount());
        }
        else{
            user = findUserByUsername(loginRequestDTO.getAccount());
        }
        // 与改密、禁用、角色修改串行，拿到锁后使用最新的账号状态和密码继续校验。
        if (user != null) {
            user = userMapper.selectByIdForUpdate(user.getId());
        }
        if (user == null) {
            log.warn(
                    "security_event=LOGIN_FAILED description=\"登录失败：用户不存在\" outcome=FAIL reason=USER_NOT_FOUND account={}",
                    maskAccount(loginRequestDTO.getAccount())
            );
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }

        // 🍰2. 密码是否正确、用户是否状态正常（被禁用则不能登录）
        if (!passwordEncoder.matches(loginRequestDTO.getPassword(), user.getPasswordHash())) {
            log.warn(
                    "security_event=LOGIN_FAILED description=\"登录失败：密码错误\" outcome=FAIL reason=PASSWORD_ERROR account={}",
                    maskAccount(loginRequestDTO.getAccount())
            );
            throw new BizException(ResultCode.PASSWORD_ERROR);
        }
        if (user.getStatus() == UserStatus.DISABLED) {
            log.warn(
                    "security_event=LOGIN_FAILED description=\"登录失败：用户已禁用\" outcome=FAIL reason=USER_DISABLED account={}",
                    maskAccount(loginRequestDTO.getAccount())
            );
            throw new BizException(ResultCode.USER_DISABLED);
        }

        // 🍰3. 生成 AccessToken 和 RefreshToken 以及各自的过期时间，并将 RefreshToken 存入数据库
        AuthTokenPair authTokenPair = generateTokenPairAndSaveRT(user);

        // 🍰4. 更新用户的登录时间
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setLastLoginAt(OffsetDateTime.now(ZoneOffset.UTC));
        userMapper.updateById(updateUser);

        log.info(
                "security_event=LOGIN_SUCCESS description=\"用户登录成功\" outcome=SUCCESS userId={} username={}",
                user.getId(),
                user.getUsername()
        );

        // 🍰5. 封装返回
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
     * 刷新登录态（RT和AT）（旧 RT 换新 RT，只轮转当前会话。）
     * @param refreshTokenRequestDTO
     * @return
     */
    @Override
    @Transactional
    public RefreshTokenVO refresh(RefreshTokenRequestDTO refreshTokenRequestDTO) {
        // 🍰1. 解析并验证 DTO 中的旧 RT 是否有效（包括判断token_type、是否存在、是否ACTIVE可用、是否没过期、是否和数据库中的token_hash相等）
        String refreshToken = refreshTokenRequestDTO.getRefreshToken();
        ValidatedRefreshToken validatedRefreshToken;
        try {
            validatedRefreshToken = validateRefreshToken(refreshToken);
        } catch (BizException ex) {
            log.warn("security_event=TOKEN_REFRESH_FAILED description=\"刷新登录态失败：Refresh Token 无效或已过期\" outcome=FAIL reason=INVALID_OR_EXPIRED");
            throw ex;
        }

        // 🍰2. 锁定用户记录，使刷新、改密、禁用和角色修改按顺序执行
        User user = userMapper.selectByIdForUpdate(validatedRefreshToken.getUserId());
        if (user == null) {
            log.warn(
                    "security_event=TOKEN_REFRESH_FAILED description=\"刷新登录态失败：用户不存在\" outcome=FAIL reason=USER_NOT_FOUND userId={}",
                    validatedRefreshToken.getUserId()
            );
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        if (user.getStatus() == UserStatus.DISABLED){
            log.warn(
                    "security_event=TOKEN_REFRESH_FAILED description=\"刷新登录态失败：用户已禁用\" outcome=FAIL reason=USER_DISABLED userId={}",
                    user.getId()
            );
            throw new BizException(ResultCode.USER_DISABLED);
        }

        // 等待用户行锁期间旧 RT 可能已被改密或另一次刷新撤销，需要在锁内重新校验。
        try {
            validatedRefreshToken = validateRefreshToken(refreshToken);
        } catch (BizException ex) {
            log.warn("security_event=TOKEN_REFRESH_FAILED description=\"刷新登录态失败：Refresh Token 已被撤销\" outcome=FAIL reason=REVOKED_WHILE_WAITING");
            throw ex;
        }

        // 🍰3. 撤销旧 RT
        revokeRefreshToken(validatedRefreshToken.getTokenJti());

        // 🍰4. 颁发新的 RT 和 AT，并且将新 RT 存入数据库
        AuthTokenPair authTokenPair = generateTokenPairAndSaveRT(user);

        log.info("security_event=TOKEN_REFRESH_SUCCESS description=\"登录态刷新成功\" outcome=SUCCESS userId={}", user.getId());

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
    public void logout(LogoutRequestDTO logoutRequestDTO) {
        if (logoutRequestDTO.getRefreshToken() == null || logoutRequestDTO.getRefreshToken().isBlank()) {
            return;
        }

        try {
            ValidatedRefreshToken validatedRefreshToken = validateRefreshToken(logoutRequestDTO.getRefreshToken());
            revokeRefreshToken(validatedRefreshToken.getTokenJti());   // 该用户退出登录后撤销其 RT
            log.info(
                    "security_event=LOGOUT_SUCCESS description=\"用户退出登录成功\" outcome=SUCCESS userId={}",
                    validatedRefreshToken.getUserId()
            );
        } catch (BizException ex) {
            if (ex.getResultCode() != ResultCode.REFRESH_TOKEN_INVALID_OR_EXPIRED) {
                throw ex;
            }
            // 退出登录按幂等语义处理：RT 无效、过期、已撤销或找不到会话，都视为已经退出。
            log.debug("security_event=LOGOUT_SUCCESS description=\"退出登录幂等完成：会话已失效\" outcome=SUCCESS reason=SESSION_ALREADY_INVALID");
        }
    }

    /**
     * 账号信息脱敏（用户名不脱敏，邮箱则部分隐藏）
     * @param account
     * @return
     */
    private String maskAccount(String account) {
        if (account == null || account.isBlank()) {
            return "";
        }

        String trimmed = account.trim();
        int atIndex = trimmed.indexOf('@');
        if (atIndex > 0) {
            String local = trimmed.substring(0, atIndex);
            String domain = trimmed.substring(atIndex);
            if (local.length() <= 2) {
                return "*".repeat(local.length()) + domain;
            }
            return local.charAt(0) + "***" + local.charAt(local.length() - 1) + domain;
        }

        if (trimmed.length() <= 2) {
            return "*".repeat(trimmed.length());
        }
        return trimmed.charAt(0) + "***" + trimmed.charAt(trimmed.length() - 1);
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
     * 指定 jti 撤销 RefreshToken
     * @param jti
     */
    @Override
    public void revokeRefreshToken(String jti) {
        authMapper.revokeRefreshToken(jti, AuthSessionStatus.REVOKED.getValue());
    }

    /**
     * 撤销指定用户所有活跃的 Refresh Token 会话
     * @param userId
     */
    @Override
    public void revokeUserRefreshTokens(Long userId) {
        authMapper.revokeActiveRefreshTokensByUserId(
                userId,
                AuthSessionStatus.ACTIVE.getValue(),
                AuthSessionStatus.REVOKED.getValue()
        );
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
                // role/status 在这里写进 Access Token，后续 JwtAuthenticationFilter 会从 Token 中读出。
                user.getRole().getValue(),
                user.getStatus().getValue(),
                user.getTokenVersion() == null ? 0L : user.getTokenVersion()
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
