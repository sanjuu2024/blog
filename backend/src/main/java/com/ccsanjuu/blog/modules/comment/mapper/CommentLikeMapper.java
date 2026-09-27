package com.ccsanjuu.blog.modules.comment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccsanjuu.blog.modules.comment.model.entity.CommentLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CommentLikeMapper extends BaseMapper<CommentLike> {

    /**
     * 查询当前用户已点赞的评论 ID。
     *
     * @param commentIds 评论 ID 列表
     * @param userId 当前用户 ID
     * @return 已点赞的评论 ID 列表
     */
    List<Long> selectLikedCommentIds(
            @Param("commentIds") List<Long> commentIds,
            @Param("userId") Long userId
    );

    /**
     * 查询当前用户对指定评论的点赞记录。
     *
     * @param commentId 评论 ID
     * @param userId 用户 ID
     * @return 点赞记录，不存在时返回 {@code null}
     */
    CommentLike selectByCommentAndUser(
            @Param("commentId") Long commentId,
            @Param("userId") Long userId
    );

    /**
     * 插入评论点赞记录；重复点赞时保持幂等。
     *
     * @param commentLike 点赞记录
     * @return 插入行数
     */
    int insertIgnore(CommentLike commentLike);

    /**
     * 删除当前用户对指定评论的点赞记录。
     *
     * @param commentId 评论 ID
     * @param userId 用户 ID
     * @return 删除行数
     */
    int deleteByCommentAndUser(
            @Param("commentId") Long commentId,
            @Param("userId") Long userId
    );
}
