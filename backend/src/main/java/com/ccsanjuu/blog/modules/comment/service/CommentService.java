package com.ccsanjuu.blog.modules.comment.service;

import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.modules.comment.model.dto.AdminCommentQueryDTO;
import com.ccsanjuu.blog.modules.comment.model.dto.CommentModerationRequestDTO;
import com.ccsanjuu.blog.modules.comment.model.vo.AdminCommentItemVO;
import com.ccsanjuu.blog.modules.comment.model.vo.CommentMutationVO;
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
}
