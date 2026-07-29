package com.ccsanjuu.blog.modules.comment.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.article.mapper.ArticleMapper;
import com.ccsanjuu.blog.modules.article.model.entity.Article;
import com.ccsanjuu.blog.modules.comment.mapper.CommentMapper;
import com.ccsanjuu.blog.modules.comment.model.dto.AdminCommentQueryDTO;
import com.ccsanjuu.blog.modules.comment.model.dto.CommentModerationRequestDTO;
import com.ccsanjuu.blog.modules.comment.model.entity.Comment;
import com.ccsanjuu.blog.modules.comment.model.enums.CommentModerationAction;
import com.ccsanjuu.blog.modules.comment.model.enums.CommentStatus;
import com.ccsanjuu.blog.modules.comment.model.enums.CommentType;
import com.ccsanjuu.blog.modules.comment.model.vo.AdminCommentArticleVO;
import com.ccsanjuu.blog.modules.comment.model.vo.AdminCommentItemVO;
import com.ccsanjuu.blog.modules.comment.model.vo.CommentAuthorVO;
import com.ccsanjuu.blog.modules.comment.model.vo.CommentMutationVO;
import com.ccsanjuu.blog.modules.comment.service.CommentService;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl extends ServiceImpl<CommentMapper,Comment> implements CommentService {

    private final ArticleMapper articleMapper;
    private final CommentMapper commentMapper;
    private final UserMapper userMapper;

    /**
     * 获取后台评论分页列表
     *
     * @param queryDTO
     * @return
     */
    @Override
    public PageResult<AdminCommentItemVO> getAdminCommentList(AdminCommentQueryDTO queryDTO) {
        // 1. 构建分页
        Page<Comment> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());

        // 2. 校验条件合法性
        Article article = null;
        if (queryDTO.getArticleId() != null){
            article = articleMapper.selectById(queryDTO.getArticleId());
            if (article == null){
                throw new BizException(ResultCode.ARTICLE_NOT_FOUND);
            }
        }

        User user = null;
        if (queryDTO.getUserId() != null){
            user = userMapper.selectById(queryDTO.getUserId());
            if (user == null){
                throw new BizException(ResultCode.USER_NOT_FOUND);
            }
        }

        // 3. 获取评论基本信息的分页列表
        lambdaQuery()
                .eq(queryDTO.getArticleId() != null, Comment::getArticleId, queryDTO.getArticleId())
                .eq(queryDTO.getUserId() != null, Comment::getUserId, queryDTO.getUserId())
                .eq(queryDTO.getStatus() != null, Comment::getStatus , queryDTO.getStatus())
                .isNull(queryDTO.getType() != null && queryDTO.getType().equals(CommentType.TOP_LEVEL), Comment::getParentId)
                .isNotNull(queryDTO.getType() != null && queryDTO.getType().equals(CommentType.REPLY), Comment::getParentId)
                .orderByDesc(Comment::getCreatedAt)
                .orderByDesc(Comment::getId)
                .page(page);
        List<Comment> records = page.getRecords();

        // 3.1 获取这些评论涉及的相关文章的信息
        List<Long> articleIds = records.stream().map(Comment::getArticleId).distinct().toList();
        Map<Long, Article> articleMap = articleIds.isEmpty()
                ? Map.of()
                : articleMapper.selectByIds(articleIds).stream().collect(Collectors.toMap(Article::getId, a -> a));

        // 3.2 获取这些评论涉及的相关用户的信息
        List<Long> userIds = records.stream().map(Comment::getUserId).distinct().toList();
        Map<Long, User> userMap = userIds.isEmpty()
                ? Map.of()
                : userMapper.selectByIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u));

        // 4. 组装
        List<AdminCommentItemVO> res = new ArrayList<>();
        records.forEach(c -> {
            AdminCommentItemVO tmp = BeanUtil.copyProperties(c, AdminCommentItemVO.class);
            tmp.setArticle(BeanUtil.copyProperties(articleMap.get(c.getArticleId()), AdminCommentArticleVO.class));
            tmp.setAuthor(BeanUtil.copyProperties(userMap.get(c.getUserId()), CommentAuthorVO.class));
            if (c.getParentId() == null){
                tmp.setType(CommentType.TOP_LEVEL);
            } else {
                tmp.setType(CommentType.REPLY);
            }
            res.add(tmp);
        });

        // 5. 返回
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), res);
    }


    /**
     * 审核、隐藏或删除评论
     *
     * @param commentId
     * @param userId
     * @param commentModerationRequestDTO
     * @return
     */
    @Override
    @Transactional
    public CommentMutationVO moderateComment(Long commentId, Long userId, CommentModerationRequestDTO commentModerationRequestDTO) {
        // 1. 评论是否存在
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null){
            throw new BizException(ResultCode.COMMENT_NOT_FOUND);
        }

        // 2. 状态流转检验
        CommentModerationAction action = commentModerationRequestDTO.getAction();
        String reason = commentModerationRequestDTO.getReason();
        CommentStatus status = comment.getStatus();
        if (!canModerate(status, action)) {
            throw new BizException(ResultCode.COMMENT_STATUS_TRANSITION_INVALID);
        }
        CommentStatus targetStatus = moderationActionToCommentStatus(action);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        // 3. 更新
        if (action == CommentModerationAction.REJECT || action == CommentModerationAction.HIDE || action == CommentModerationAction.DELETE) {
            if (!StringUtils.hasText(reason)){
                throw new BizException(ResultCode.COMMENT_MODERATION_REASON_REQUIRED);
            }
            String trimmedReason = reason.trim();

            if (action == CommentModerationAction.DELETE) {
                deleteCommentSubtree(comment, userId, trimmedReason, now);
            } else {
                int updatedRows = commentMapper.update(
                        new LambdaUpdateWrapper<Comment>()
                                .eq(Comment::getId, commentId)
                                .eq(Comment::getStatus, status)
                                .set(Comment::getModerationReason, trimmedReason)
                                .set(Comment::getStatus, targetStatus)
                                .set(Comment::getReviewedBy, userId)
                                .set(Comment::getReviewedAt, now)
                );
                if (updatedRows != 1) {
                    throw new BizException(ResultCode.COMMENT_STATUS_TRANSITION_INVALID);
                }
                updateArticleCommentCount(comment.getArticleId(), calculateCommentCountDelta(status, targetStatus));
            }
        }

        else {
            // APPROVED
            int updatedRows = commentMapper.update(
                    new LambdaUpdateWrapper<Comment>()
                            .eq(Comment::getId, commentId)
                            .eq(Comment::getStatus, status)
                            .set(Comment::getModerationReason, null)
                            .set(Comment::getStatus, targetStatus)
                            .set(Comment::getReviewedBy, userId)
                            .set(Comment::getReviewedAt, now)
            );
            if (updatedRows != 1) {
                throw new BizException(ResultCode.COMMENT_STATUS_TRANSITION_INVALID);
            }
            updateArticleCommentCount(comment.getArticleId(), calculateCommentCountDelta(status, targetStatus));
        }

        // 4. 查询返回
        return BeanUtil.copyProperties(commentMapper.selectById(commentId), CommentMutationVO.class);
    }


    /**
     * 判断当前评论状态是否允许执行指定审核动作。
     *
     * @param status 当前评论状态
     * @param action 审核动作
     * @return 允许执行则返回 true，否则返回 false
     */
    private boolean canModerate(CommentStatus status, CommentModerationAction action) {
        return switch (action) {
            case APPROVE -> status == CommentStatus.PENDING
                    || status == CommentStatus.REJECTED
                    || status == CommentStatus.HIDDEN;
            case REJECT -> status == CommentStatus.PENDING;
            case HIDE -> status == CommentStatus.APPROVED;
            case DELETE -> status != CommentStatus.DELETED;
        };
    }

    /**
     * 将审核动作转换为动作执行后的目标评论状态。
     *
     * @param action 审核动作
     * @return 目标评论状态
     */
    private CommentStatus moderationActionToCommentStatus(CommentModerationAction action){
        switch (action){
            case APPROVE:
                return CommentStatus.APPROVED;
            case REJECT:
                return CommentStatus.REJECTED;
            case HIDE:
                return CommentStatus.HIDDEN;
            default:   // case DELETE
                return CommentStatus.DELETED;
        }
    }

    /**
     * 逻辑删除指定评论及其所有子孙回复，并同步扣减文章已通过评论数。
     *
     * @param comment 待删除的评论
     * @param userId 执行删除操作的管理员用户 ID
     * @param reason 删除原因
     * @param now 删除时间
     */
    private void deleteCommentSubtree(Comment comment, Long userId, String reason, OffsetDateTime now) {
        List<Long> commentIds = getCommentSubtreeIds(comment.getId());
        long approvedCount = lambdaQuery()
                .in(Comment::getId, commentIds)
                .eq(Comment::getStatus, CommentStatus.APPROVED)
                .count();

        int updatedRows = commentMapper.update(
                new LambdaUpdateWrapper<Comment>()
                        .eq(Comment::getId, comment.getId())
                        .eq(Comment::getStatus, comment.getStatus())
                        .set(Comment::getModerationReason, reason)
                        .set(Comment::getStatus, CommentStatus.DELETED)
                        .set(Comment::getDeletedBy, userId)
                        .set(Comment::getDeletedAt, now)
        );
        if (updatedRows != 1) {
            throw new BizException(ResultCode.COMMENT_STATUS_TRANSITION_INVALID);
        }

        List<Long> childIds = commentIds.stream()
                .filter(id -> !id.equals(comment.getId()))
                .toList();
        if (!childIds.isEmpty()) {
            commentMapper.update(
                    new LambdaUpdateWrapper<Comment>()
                            .in(Comment::getId, childIds)
                            .ne(Comment::getStatus, CommentStatus.DELETED)
                            .set(Comment::getModerationReason, reason)
                            .set(Comment::getStatus, CommentStatus.DELETED)
                            .set(Comment::getDeletedBy, userId)
                            .set(Comment::getDeletedAt, now)
            );
        }
        updateArticleCommentCount(comment.getArticleId(), -approvedCount);
    }

    /**
     * 查询指定评论为根的整棵评论子树 ID 列表，包含自身和所有层级子回复。
     *
     * @param commentId 根评论 ID
     * @return 评论子树 ID 列表
     */
    private List<Long> getCommentSubtreeIds(Long commentId) {
        List<Long> commentIds = new ArrayList<>();
        commentIds.add(commentId);

        List<Long> currentParentIds = List.of(commentId);
        while (!currentParentIds.isEmpty()) {
            List<Long> childIds = lambdaQuery()
                    .select(Comment::getId)
                    .in(Comment::getParentId, currentParentIds)
                    .list()
                    .stream()
                    .map(Comment::getId)
                    .toList();
            commentIds.addAll(childIds);
            currentParentIds = childIds;
        }
        return commentIds;
    }

    /**
     * 根据评论状态变化计算文章 comment_count 的增量。
     *
     * @param oldStatus 原评论状态
     * @param newStatus 新评论状态
     * @return 进入 APPROVED 时返回 1，离开 APPROVED 时返回 -1，其他情况返回 0
     */
    private long calculateCommentCountDelta(CommentStatus oldStatus, CommentStatus newStatus) {
        if (oldStatus != CommentStatus.APPROVED && newStatus == CommentStatus.APPROVED) {
            return 1;
        }
        if (oldStatus == CommentStatus.APPROVED && newStatus != CommentStatus.APPROVED) {
            return -1;
        }
        return 0;
    }

    /**
     * 按增量更新文章评论数，并在扣减时保证 comment_count 不会小于 0。
     *
     * @param articleId 文章 ID
     * @param delta 评论数增量
     */
    private void updateArticleCommentCount(Long articleId, long delta) {
        if (delta == 0) {
            return;
        }

        LambdaUpdateWrapper<Article> wrapper = new LambdaUpdateWrapper<Article>()
                .eq(Article::getId, articleId);
        if (delta > 0) {
            wrapper.setSql("comment_count = comment_count + " + delta);
        } else {
            wrapper.setSql("comment_count = GREATEST(comment_count - " + Math.abs(delta) + ", 0)");
        }
        articleMapper.update(null, wrapper);
    }
}
