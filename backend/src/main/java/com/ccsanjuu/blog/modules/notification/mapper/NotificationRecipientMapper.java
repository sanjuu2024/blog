package com.ccsanjuu.blog.modules.notification.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccsanjuu.blog.modules.notification.model.entity.NotificationRecipient;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NotificationRecipientMapper extends BaseMapper<NotificationRecipient> {

    /**
     * 查询用户符合分类条件的通知收件记录 ID。
     *
     * @param userId 用户 ID
     * @param category 通知分类
     * @return 收件记录 ID 列表
     */
    List<Long> selectIdsForRead(@Param("userId") Long userId, @Param("category") String category);
}
