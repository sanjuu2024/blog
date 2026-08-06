package com.ccsanjuu.blog.modules.comment.service;

import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.modules.comment.model.dto.AdminCommentQueryDTO;
import com.ccsanjuu.blog.modules.comment.model.dto.CommentModerationRequestDTO;
import com.ccsanjuu.blog.modules.comment.model.dto.CommentReplyQueryDTO;
import com.ccsanjuu.blog.modules.comment.model.dto.CreateCommentRequestDTO;
import com.ccsanjuu.blog.modules.comment.model.dto.PublicCommentQueryDTO;
import com.ccsanjuu.blog.modules.comment.model.vo.AdminCommentItemVO;
import com.ccsanjuu.blog.modules.comment.model.vo.CommentDeleteVO;
import com.ccsanjuu.blog.modules.comment.model.vo.CommentMutationVO;
import com.ccsanjuu.blog.modules.comment.model.vo.CommentReplyPageVO;
import com.ccsanjuu.blog.modules.comment.model.vo.PublicCommentItemVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

public interface CommentService {
    /**
     * 获取后台评论分页列表
     * @param adminCommentQueryDTO
     * @return
     */
    PageResult<AdminCommentItemVO> getAdminCommentList(@Valid AdminCommentQueryDTO adminCommentQueryDTO);

    /**
     * 审核、隐藏或删除评论
     * @param commentId
     * @param userId
     * @param commentModerationRequestDTO
     * @return
     */
    CommentMutationVO moderateComment(@Positive Long commentId, Long userId, CommentModerationRequestDTO commentModerationRequestDTO);

    /**
     * 获取顶层评论下的回复
     * @param commentId
     * @param currentUserId
     * @param dto
     * @return
     */
    CommentReplyPageVO getRepliesByRootId(@Positive Long commentId, Long currentUserId, @Valid CommentReplyQueryDTO dto);

    /**
     * 获取文章顶层评论分页列表
     * @param articleId
     * @param currentUserId
     * @param publicCommentQueryDTO
     * @return
     */
    PageResult<PublicCommentItemVO> getPublicCommentList(@Positive Long articleId, Long currentUserId, @Valid PublicCommentQueryDTO publicCommentQueryDTO);

    /**
     * 发表评论或回复
     * @param articleId
     * @param userId
     * @param createCommentRequestDTO
     * @return
     */
    CommentMutationVO createComment(@Positive Long articleId, Long userId, @Valid CreateCommentRequestDTO createCommentRequestDTO);

    /**
     * 删除自己的评论
     * @param commentId
     * @param userId
     */
    CommentDeleteVO deleteOwnComment(@Positive Long commentId, Long userId);
}
