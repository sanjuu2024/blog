package com.ccsanjuu.blog.modules.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccsanjuu.blog.modules.message.model.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    /**
     * 在事务内串行化公告发布，确保只有最新公告置顶。
     *
     * @return 固定值 1
     */
    int lockAnnouncementPublishing();

    Message selectByIdForUpdate(@Param("messageId") Long messageId);

    List<Long> selectSubtreeIds(@Param("messageId") Long messageId);

    List<Message> selectPendingRootsForUpdate(@Param("messageIds") List<Long> messageIds);
}
