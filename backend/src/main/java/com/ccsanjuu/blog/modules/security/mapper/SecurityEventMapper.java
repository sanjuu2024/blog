package com.ccsanjuu.blog.modules.security.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccsanjuu.blog.modules.security.model.entity.SecurityEvent;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.time.OffsetDateTime;

public interface SecurityEventMapper extends BaseMapper<SecurityEvent> {

    /**
     * 分批删除超过保留期的安全事件。
     *
     * @param cutoff 保留边界
     * @param batchSize 单批上限
     * @return 删除数量
     */
    @Delete("DELETE FROM blog_security_event WHERE id IN (SELECT id FROM blog_security_event "
            + "WHERE created_at < #{cutoff} ORDER BY created_at, id LIMIT #{batchSize})")
    int deleteExpiredBatch(@Param("cutoff") OffsetDateTime cutoff, @Param("batchSize") int batchSize);
}
