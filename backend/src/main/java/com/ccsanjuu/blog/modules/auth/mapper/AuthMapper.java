package com.ccsanjuu.blog.modules.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccsanjuu.blog.modules.auth.model.entity.AuthSession;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface AuthMapper extends BaseMapper<AuthSession> {
    /**
     * 指定 jti 撤销 RefreshToken
     * @param jti
     */
    @Update("""
            UPDATE blog_auth_session
            SET status = #{revokeStatus}, revoked_at = NOW(), updated_at = NOW()
            WHERE token_jti = #{jti}
            """)
    void revokeRefreshToken(@Param("jti") String jti, @Param("revokeStatus") String revokeStatus);

    /**
     * 撤销指定用户所有活跃的 Refresh Token 会话
     * @param userId
     * @param activeStatus
     * @param revokeStatus
     */
    @Update("""
            UPDATE blog_auth_session
            SET status = #{revokeStatus}, revoked_at = NOW(), updated_at = NOW()
            WHERE user_id = #{userId}
              AND token_type = 'REFRESH'
              AND status = #{activeStatus}
            """)
    void revokeActiveRefreshTokensByUserId(
            @Param("userId") Long userId,
            @Param("activeStatus") String activeStatus,
            @Param("revokeStatus") String revokeStatus
    );
}
