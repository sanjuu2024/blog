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

    /**
     * 锁定单条留言，避免并发审核、删除或回复交错更新。
     *
     * @param messageId 留言 ID
     * @return 留言数据
     */
    Message selectByIdForUpdate(@Param("messageId") Long messageId);

    /**
     * 查询指定留言及其全部后代 ID。
     *
     * @param messageId 留言 ID
     * @return 留言树 ID 列表
     */
    List<Long> selectSubtreeIds(@Param("messageId") Long messageId);

    /**
     * 锁定待审核的顶层留言。
     *
     * @param messageIds 留言 ID 列表
     * @return 待审核顶层留言
     */
    List<Message> selectPendingRootsForUpdate(@Param("messageIds") List<Long> messageIds);

}
