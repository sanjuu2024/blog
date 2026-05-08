package com.ccsanjuu.blog.modules.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccsanjuu.blog.modules.auth.model.entity.AuthSession;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

public interface AuthMapper extends BaseMapper<AuthSession> {
    /**
     * 撤销旧的 RefreshToken
     * @param jti
     */
    @Update("""
            UPDATE blog_auth_session
            SET status = #{tokenStatus}, revoked_at = NOW(), updated_at = NOW()
            WHERE token_jti = #{jti}
            """)
    void revokeRefreshToken(@Param("jti") String jti, @Param("tokenStatus") String tokenStatus);
}
