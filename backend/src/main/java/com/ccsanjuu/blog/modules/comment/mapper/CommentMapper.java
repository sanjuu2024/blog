package com.ccsanjuu.blog.modules.comment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ccsanjuu.blog.modules.comment.model.entity.Comment;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CommentMapper extends BaseMapper<Comment> {

    List<Long> selectCommentSubtreeIds(@Param("commentId") Long commentId);

    long countApprovedCommentSubtree(@Param("commentId") Long commentId);
}
