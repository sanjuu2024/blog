package com.ccsanjuu.blog.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.MacAlgorithm;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Objects;

/**
 * 基于 jjwt 0.12.x HMAC API 的 JWT 工具类。
 * <p>
 * 这里只依赖 {@code jjwt-api} 可见的类型。由于项目将
 * {@code jjwt-impl} 和 {@code jjwt-jackson} 声明为 runtime 依赖，
 * 工具代码应依赖 JJWT 的运行时自动发现机制，而不是直接导入适配器实现类。
 */
public final class JwtUtil {

    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_USERNAME = "username";
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_STATUS = "status";
    public static final String CLAIM_TOKEN_VERSION = "tokenVersion";
    public static final String CLAIM_TOKEN_TYPE = "tokenType";

    public static final String TOKEN_TYPE_ACCESS = "ACCESS";
    public static final String TOKEN_TYPE_REFRESH = "REFRESH";

    private static final MacAlgorithm DEFAULT_MAC_ALGORITHM = Jwts.SIG.HS256;
    private static final int MIN_HMAC_SECRET_LENGTH = 32;

    private JwtUtil() {
    }

    /**
     * 使用原始文本密钥创建 HMAC Key。
     *
     * @param secret 原始密钥文本，使用 HS256 时至少需要 32 字节
     * @return HMAC 签名 Key
     */
    public static SecretKey createHmacShaKey(String secret) {
        requireText(secret, "secret");
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_HMAC_SECRET_LENGTH) {
            throw new IllegalArgumentException("secret must be at least 32 bytes for HS256");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 使用 Base64 编码后的密钥创建 HMAC Key。
     *
     * @param base64Secret Base64 形式的签名密钥
     * @return HMAC 签名 Key
     */
    public static SecretKey createHmacShaKeyFromBase64(String base64Secret) {
        requireText(base64Secret, "base64Secret");
        byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
        if (keyBytes.length < MIN_HMAC_SECRET_LENGTH) {
            throw new IllegalArgumentException("decoded secret must be at least 32 bytes for HS256");
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成携带当前用户身份信息的 Access Token。
     *
     * @param signingKey 用于签名和后续验签的 HMAC Key
     * @param issuer Token 签发方，通常是应用名；为空时不写入 {@code iss}
     * @param ttl Token 从当前时间开始计算的有效期
     * @param userId 用户主键 ID，同时也会写入 {@code sub} 作为稳定身份标识
     * @param username 当前用户名快照，写入 claims 供后续读取
     * @param role 当前用户角色，例如 {@code ADMIN} 或 {@code USER}
     * @param status 当前用户状态，例如 {@code ACTIVE} 或 {@code DISABLED}
     * @return 已签名的紧凑 JWT 字符串
     */
    public static String generateAccessToken(
            SecretKey signingKey,
            String issuer,
            Duration ttl,
            Long userId,
            String username,
            String role,
            String status
    ) {
        return generateAccessToken(signingKey, issuer, ttl, userId, username, role, status, 0L);
    }

    /**
     * 生成携带当前用户 tokenVersion 的 Access Token。
     *
     * @param signingKey 用于签名和后续验签的 HMAC Key
     * @param issuer Token 签发方，通常是应用名；为空时不写入 {@code iss}
     * @param ttl Token 从当前时间开始计算的有效期
     * @param userId 用户主键 ID，同时也会写入 {@code sub} 作为稳定身份标识
     * @param username 当前用户名快照，写入 claims 供后续读取
     * @param role 当前用户角色，例如 {@code ADMIN} 或 {@code USER}
     * @param status 当前用户状态，例如 {@code ACTIVE} 或 {@code DISABLED}
     * @param tokenVersion 签发时的用户令牌版本
     * @return 已签名的紧凑 JWT 字符串
     */
    public static String generateAccessToken(
            SecretKey signingKey,
            String issuer,
            Duration ttl,
            Long userId,
            String username,
            String role,
            String status,
            Long tokenVersion
    ) {
        Map<String, Object> claims = Map.of(
                CLAIM_USER_ID, userId,
                CLAIM_USERNAME, username,
                CLAIM_ROLE, role,
                CLAIM_STATUS, status,
                CLAIM_TOKEN_VERSION, tokenVersion == null ? 0L : tokenVersion,
                CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS
        );
        // 使用稳定的 userId 作为 subject，避免用户名变更影响身份识别。
        return generateToken(signingKey, issuer, ttl, String.valueOf(userId), null, claims);
    }

    /**
     * 生成用于续期登录态的 Refresh Token。
     *
     * @param signingKey 用于签名和后续验签的 HMAC Key
     * @param issuer Token 签发方，通常是应用名
     * @param ttl Token 从当前时间开始计算的有效期
     * @param userId 用户主键 ID，同时也会写入 {@code sub}
     * @param jti Token 唯一 ID（JWT ID），通常对应 {@code blog_auth_session.token_jti}
     * @return 已签名的紧凑 JWT 字符串
     */
    public static String generateRefreshToken(
            SecretKey signingKey,
            String issuer,
            Duration ttl,
            Long userId,
            String jti
    ) {
        Map<String, Object> claims = Map.of(
                CLAIM_USER_ID, userId,
                CLAIM_TOKEN_TYPE, TOKEN_TYPE_REFRESH
        );
        return generateToken(signingKey, issuer, ttl, String.valueOf(userId), jti, claims);
    }

    /**
     * 生成通用的签名 Token。
     *
     * @param signingKey 用于签名和后续验签的 HMAC Key
     * @param issuer Token 签发方，为空时不写入 {@code iss} claim
     * @param ttl Token 从当前时间开始计算的有效期
     * @param subject Token 主题，通常建议使用稳定的主体 ID，而不是可变的用户名
     * @param jti Token 唯一 ID（JWT ID），可用于撤销、追踪或和会话记录绑定；为空时不写入
     * @param claims 需要写入负载的自定义 claims，值为 null 的条目会被忽略
     * @return 已签名的紧凑 JWT 字符串
     */
    public static String generateToken(
            SecretKey signingKey,
            String issuer,
            Duration ttl,
            String subject,
            String jti,
            Map<String, ?> claims
    ) {
        Objects.requireNonNull(signingKey, "signingKey must not be null");
        Objects.requireNonNull(ttl, "ttl must not be null");
        requireText(subject, "subject");

        Instant issuedAt = Instant.now();
        Instant expirationAt = issuedAt.plus(ttl);

        JwtBuilder builder = Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expirationAt))
                .signWith(signingKey, DEFAULT_MAC_ALGORITHM);

        if (hasText(issuer)) {
            builder.issuer(issuer);
        }
        if (hasText(jti)) {
            builder.id(jti);
        }
        if (claims != null && !claims.isEmpty()) {
            claims.forEach((key, value) -> {
                if (value != null) {
                    builder.claim(key, value);
                }
            });
        }

        return builder.compact();
    }

    /**
     * 解析紧凑 JWT，并校验签名是否合法。
     *
     * @param token 紧凑格式的 JWT 字符串
     * @param signingKey 用于验签的 HMAC Key
     * @return 解析后的签名 claims 结构
     */
    public static Jws<Claims> parseSignedClaims(String token, SecretKey signingKey) {
        requireText(token, "token");
        Objects.requireNonNull(signingKey, "signingKey must not be null");
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token);
    }

    /**
     * 解析并只返回 claims 负载。
     *
     * @param token 紧凑格式的 JWT 字符串
     * @param signingKey 用于验签的 HMAC Key
     * @return JWT claims 负载
     */
    public static Claims parseClaims(String token, SecretKey signingKey) {
        return parseSignedClaims(token, signingKey).getPayload();
    }

    public static Long getUserId(String token, SecretKey signingKey) {
        return getUserId(parseClaims(token, signingKey));
    }

    public static Long getUserId(Claims claims) {
        Objects.requireNonNull(claims, "claims must not be null");
        Object userId = claims.get(CLAIM_USER_ID);
        if (userId instanceof Number number) {
            return number.longValue();
        }
        if (userId instanceof String text && hasText(text)) {
            return Long.parseLong(text);
        }
        // 兼容只把用户 ID 写在 subject 中的旧 token 结构。
        return Long.parseLong(claims.getSubject());
    }

    public static String getUsername(String token, SecretKey signingKey) {
        return parseClaims(token, signingKey).get(CLAIM_USERNAME, String.class);
    }

    public static String getRole(String token, SecretKey signingKey) {
        return parseClaims(token, signingKey).get(CLAIM_ROLE, String.class);
    }

    public static String getStatus(String token, SecretKey signingKey) {
        return parseClaims(token, signingKey).get(CLAIM_STATUS, String.class);
    }

    /**
     * 获取 Access Token 签发时的 tokenVersion；旧 Token 缺失该 claim 时按 0 兼容。
     *
     * @param claims JWT claims
     * @return 签发时的 tokenVersion
     */
    public static Long getTokenVersion(Claims claims) {
        Objects.requireNonNull(claims, "claims must not be null");
        Object tokenVersion = claims.get(CLAIM_TOKEN_VERSION);
        if (tokenVersion instanceof Number number) {
            return number.longValue();
        }
        if (tokenVersion instanceof String text && hasText(text)) {
            return Long.parseLong(text);
        }
        return 0L;
    }

    public static String getTokenType(String token, SecretKey signingKey) {
        return parseClaims(token, signingKey).get(CLAIM_TOKEN_TYPE, String.class);
    }

    public static String getTokenId(String token, SecretKey signingKey) {
        return parseClaims(token, signingKey).getId();
    }

    public static Instant getExpiration(String token, SecretKey signingKey) {
        return parseClaims(token, signingKey).getExpiration().toInstant();
    }

    public static boolean isAccessToken(String token, SecretKey signingKey) {
        return TOKEN_TYPE_ACCESS.equals(getTokenType(token, signingKey));
    }

    public static boolean isRefreshToken(String token, SecretKey signingKey) {
        return TOKEN_TYPE_REFRESH.equals(getTokenType(token, signingKey));
    }

    public static boolean isTokenExpired(String token, SecretKey signingKey) {
        try {
            return getExpiration(token, signingKey).isBefore(Instant.now());
        } catch (ExpiredJwtException ex) {
            // jjwt 0.12.x 会在解析阶段直接拒绝过期 token，这里统一转换为“已过期”。
            return true;
        }
    }

    public static boolean isTokenValid(
            String token,
            SecretKey signingKey,
            String expectedTokenType,
            Long expectedUserId
    ) {
        // expectedTokenType 一般传 ACCESS 或 REFRESH；为空表示跳过类型校验。
        // expectedUserId 可选，用于调用方要求确认该 token 确实属于指定用户。
        try {
            Claims claims = parseClaims(token, signingKey);
            boolean typeMatched = !hasText(expectedTokenType)
                    || expectedTokenType.equals(claims.get(CLAIM_TOKEN_TYPE, String.class));
            boolean subjectMatched = expectedUserId == null
                    || String.valueOf(expectedUserId).equals(claims.getSubject());
            return typeMatched
                    && subjectMatched
                    && claims.getExpiration() != null
                    && claims.getExpiration().toInstant().isAfter(Instant.now());
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    private static void requireText(String value, String fieldName) {
        if (!hasText(value)) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
