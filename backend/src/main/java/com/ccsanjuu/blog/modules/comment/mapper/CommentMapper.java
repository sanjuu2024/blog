package com.ccsanjuu.blog.modules.comment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccsanjuu.blog.modules.comment.model.bo.CommentReplyCountBO;
import com.ccsanjuu.blog.modules.comment.model.entity.Comment;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

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
