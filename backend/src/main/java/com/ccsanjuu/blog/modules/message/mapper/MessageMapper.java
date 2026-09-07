package com.ccsanjuu.blog.modules.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccsanjuu.blog.modules.message.model.entity.Message;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    Message selectByIdForUpdate(@Param("messageId") Long messageId);

    List<Long> selectSubtreeIds(@Param("messageId") Long messageId);

    List<Message> selectPendingRootsForUpdate(@Param("messageIds") List<Long> messageIds);
}
