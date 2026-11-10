package com.ccsanjuu.blog.modules.comment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccsanjuu.blog.modules.comment.model.bo.CommentReplyCountBO;
import com.ccsanjuu.blog.modules.comment.model.bo.CommentReplyCursorBO;
import com.ccsanjuu.blog.modules.comment.model.entity.Comment;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    /**
     * 管理员直接回复顶层评论时优先查询，其余可见回复按原有时间顺序查询。
     *
     * @param page 分页参数
     * @param rootId 顶层评论 ID
     * @param currentUserId 当前用户 ID；游客为空
     * @param cursor 回复游标；首次请求为空
     * @return 回复分页结果
     */
    IPage<Comment> selectReplyPage(
            Page<Comment> page,
            @Param("rootId") Long rootId,
            @Param("currentUserId") Long currentUserId,
            @Param("cursor") CommentReplyCursorBO cursor
    );

    Comment selectByIdForUpdate(@Param("commentId") Long commentId);

    List<Long> selectCommentSubtreeIds(@Param("commentId") Long commentId);

    long countApprovedCommentSubtree(@Param("commentId") Long commentId);

    List<CommentReplyCountBO> selectReplyCounts(
            @Param("rootIds") List<Long> rootIds,
            @Param("currentUserId") Long currentUserId
    );

    /**
     * 原子递增评论点赞数。
     *
     * @param commentId 评论 ID
     * @return 更新行数
     */
    int incrementLikeCount(@Param("commentId") Long commentId);

    /**
     * 原子递减评论点赞数，并防止冗余计数变为负数。
     *
     * @param commentId 评论 ID
     * @return 更新行数
     */
    int decrementLikeCount(@Param("commentId") Long commentId);
}
