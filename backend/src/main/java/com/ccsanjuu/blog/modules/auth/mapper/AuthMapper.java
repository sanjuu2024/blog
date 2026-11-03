package com.ccsanjuu.blog.modules.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccsanjuu.blog.modules.auth.model.entity.AuthSession;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface AuthMapper extends BaseMapper<AuthSession> {

    /**
     * 读取数据库最新会话，避免用户行锁等待前的 MyBatis 一级缓存覆盖撤销结果。
     *
     * @param tokenJti Refresh Token 会话标识
     * @param userId 所属用户 ID
     * @return 对应 Refresh Token 会话
     */
    @Select("SELECT * FROM blog_auth_session WHERE token_jti = #{tokenJti} "
            + "AND user_id = #{userId} AND token_type = 'REFRESH'")
    @Options(useCache = false, flushCache = Options.FlushCachePolicy.TRUE)
    AuthSession selectRefreshSession(@Param("tokenJti") String tokenJti, @Param("userId") Long userId);
}
