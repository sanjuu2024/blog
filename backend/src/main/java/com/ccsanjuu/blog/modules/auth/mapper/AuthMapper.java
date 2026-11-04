package com.ccsanjuu.blog.modules.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccsanjuu.blog.modules.auth.model.entity.AuthSession;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.time.OffsetDateTime;

public interface AuthMapper extends BaseMapper<AuthSession> {

    /**
     * 分批删除已超过保留期的过期会话，不按撤销状态单独删除。
     *
     * @param cutoff 过期时间边界
     * @param batchSize 单批上限
     * @return 删除数量
     */
    @Delete("DELETE FROM blog_auth_session WHERE id IN (SELECT id FROM blog_auth_session "
            + "WHERE expires_at < #{cutoff} ORDER BY expires_at, id LIMIT #{batchSize})")
    int deleteExpiredBatch(@Param("cutoff") OffsetDateTime cutoff, @Param("batchSize") int batchSize);

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
