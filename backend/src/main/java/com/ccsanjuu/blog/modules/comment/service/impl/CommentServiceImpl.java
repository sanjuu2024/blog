package com.ccsanjuu.blog.modules.comment.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.article.mapper.ArticleMapper;
import com.ccsanjuu.blog.modules.article.model.entity.Article;
import com.ccsanjuu.blog.modules.article.model.enums.ArticleStatus;
import com.ccsanjuu.blog.modules.comment.mapper.CommentMapper;
import com.ccsanjuu.blog.modules.comment.model.dto.AdminCommentQueryDTO;
import com.ccsanjuu.blog.modules.comment.model.dto.CommentModerationRequestDTO;
import com.ccsanjuu.blog.modules.comment.model.dto.CommentReplyQueryDTO;
import com.ccsanjuu.blog.modules.comment.model.dto.CreateCommentRequestDTO;
import com.ccsanjuu.blog.modules.comment.model.dto.PublicCommentQueryDTO;
import com.ccsanjuu.blog.modules.comment.model.bo.CommentReplyCursorBO;
import com.ccsanjuu.blog.modules.comment.model.entity.Comment;
import com.ccsanjuu.blog.modules.comment.model.enums.CommentModerationAction;
import com.ccsanjuu.blog.modules.comment.model.enums.CommentStatus;
import com.ccsanjuu.blog.modules.comment.model.enums.CommentType;
import com.ccsanjuu.blog.modules.comment.model.vo.*;
import com.ccsanjuu.blog.modules.comment.service.CommentService;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import com.ccsanjuu.blog.modules.user.model.enums.UserRole;
import com.ccsanjuu.blog.modules.user.model.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl extends ServiceImpl<CommentMapper,Comment> implements CommentService {

    private static final Duration COMMENT_RATE_LIMIT_DURATION = Duration.ofSeconds(10);
    private static final String COMMENT_RATE_LIMIT_KEY_PREFIX = "blog:comment:rate";

    private final ArticleMapper articleMapper;
    private final CommentMapper commentMapper;
    private final UserMapper userMapper;
    private final StringRedisTemplate stringRedisTemplate;

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
                .ge(queryDTO.getCreatedAtFrom() != null, Comment::getCreatedAt, queryDTO.getCreatedAtFrom())
                .le(queryDTO.getCreatedAtTo() != null, Comment::getCreatedAt, queryDTO.getCreatedAtTo())
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
     * 获取顶层评论下的回复
     *
     * @param commentId
     * @param currentUserId
     * @param dto
     * @return
     */
    @Override
    public CommentReplyPageVO getRepliesByRootId(Long commentId, Long currentUserId, CommentReplyQueryDTO dto) {
        // 1. 校验顶层评论是否存在、是否可见
        Comment rootComment = commentMapper.selectById(commentId);
        if (rootComment == null || rootComment.getParentId() != null || !isVisibleComment(rootComment, currentUserId)){
            throw new BizException(ResultCode.COMMENT_NOT_FOUND);
        }

        // 2. 解析游标：首次展开不传 cursor，继续加载时用上一次返回的 nextCursor
        CommentReplyCursorBO cursor = parseReplyCursor(dto.getCursor());

        // 3. 查询回复列表
        // 游标分页不查总数。这里多取 1 条：如果查到 limit + 1 条，就说明后面还有数据。
        Page<Comment> page = new Page<>(1, dto.getLimit() + 1, false);
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<Comment>()
                .eq(Comment::getRootId, commentId)
                .orderByAsc(Comment::getCreatedAt)
                .orderByAsc(Comment::getId);
        addVisibleCommentCondition(wrapper, currentUserId);
        addReplyCursorCondition(wrapper, cursor);

        commentMapper.selectPage(page, wrapper);
        List<Comment> comments = page.getRecords();

        boolean hasNext = comments.size() > dto.getLimit();
        if (hasNext){
            comments = comments.subList(0, dto.getLimit());
        }

        // 4. 封装返回
        List<CommentReplyItemVO> records = buildReplyItemVOList(comments, currentUserId);
        // nextCursor 取当前页最后一条回复的位置，下一次查询从它后面继续
        String nextCursor = hasNext ? buildReplyCursor(comments.getLast()) : null;

        return CommentReplyPageVO.builder()
                .records(records)
                .nextCursor(nextCursor)
                .hasNext(hasNext)
                .build();
    }


    /**
     * 获取文章顶层评论分页列表
     *
     * @param articleId
     * @param currentUserId
     * @param publicCommentQueryDTO
     * @return
     */
    @Override
    public PageResult<PublicCommentItemVO> getPublicCommentList(Long articleId, Long currentUserId, PublicCommentQueryDTO publicCommentQueryDTO) {
        // 1. 校验文章是否存在、是否已发布
        checkPublishedArticle(articleId);

        // 2. 查询顶层评论分页
        Page<Comment> page = Page.of(publicCommentQueryDTO.getPageNum(), publicCommentQueryDTO.getPageSize());
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<Comment>()
                .eq(Comment::getArticleId, articleId)
                .isNull(Comment::getParentId)
                .orderByDesc(Comment::getCreatedAt)
                .orderByDesc(Comment::getId);
        addVisibleCommentCondition(wrapper, currentUserId);

        commentMapper.selectPage(page, wrapper);
        List<Comment> comments = page.getRecords();
        if (comments.isEmpty()){
            return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), List.of());
        }

        // 3. 封装返回
        List<PublicCommentItemVO> records = buildPublicCommentItemVOList(comments, currentUserId);
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }


    /**
     * 发表评论或回复
     *
     * @param articleId
     * @param userId
     * @param createCommentRequestDTO
     * @return
     */
    @Override
    @Transactional
    public CommentMutationVO createComment(Long articleId, Long userId, CreateCommentRequestDTO createCommentRequestDTO) {
        // 1. 校验文章是否存在、是否允许评论
        Article article = checkPublishedArticle(articleId);
        if (article.getAllowComment() != null && !article.getAllowComment()){
            throw new BizException(ResultCode.COMMENT_DISABLED);
        }

        // 2. 校验当前用户是否可发表评论
        User user = userMapper.selectById(userId);
        if (user == null){
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        if (user.getStatus() == UserStatus.DISABLED){
            throw new BizException(ResultCode.USER_DISABLED);
        }

        // 3. 校验父评论，计算 rootId
        Comment parentComment = null;
        Long rootId = null;
        if (createCommentRequestDTO.getParentId() != null){
            parentComment = commentMapper.selectById(createCommentRequestDTO.getParentId());
            if (parentComment == null
                    || !parentComment.getArticleId().equals(articleId)
                    || parentComment.getStatus() != CommentStatus.APPROVED){
                throw new BizException(ResultCode.COMMENT_PARENT_UNAVAILABLE);
            }
            // 顶层评论的 rootId 为空；回复的 rootId 指向所属顶层评论
            rootId = parentComment.getParentId() == null ? parentComment.getId() : parentComment.getRootId();
        }

        // 4. 检查评论频率
        checkCommentRateLimit(userId, articleId);

        // 5. 创建评论
        CommentStatus status = user.getRole() == UserRole.ADMIN ? CommentStatus.APPROVED : CommentStatus.PENDING;
        Comment comment = Comment.builder()
                .articleId(articleId)
                .userId(userId)
                .parentId(createCommentRequestDTO.getParentId())
                .rootId(rootId)
                .content(createCommentRequestDTO.getContent().trim())
                .status(status)
                .build();

        commentMapper.insert(comment);
        if (status == CommentStatus.APPROVED){
            updateArticleCommentCount(articleId, 1);
        }

        // 6. 返回
        Comment newComment = commentMapper.selectById(comment.getId());
        CommentMutationVO vo = BeanUtil.copyProperties(newComment, CommentMutationVO.class);
        vo.setAuthor(BeanUtil.copyProperties(user, CommentAuthorVO.class));
        return vo;
    }


    /**
     * 删除自己的评论
     *
     * @param commentId
     * @param userId
     */
    @Override
    @Transactional
    public void deleteOwnComment(Long commentId, Long userId) {
        // 1. 校验评论是否存在
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getStatus() == CommentStatus.DELETED){
            throw new BizException(ResultCode.COMMENT_NOT_FOUND);
        }

        // 2. 校验操作权限
        if (!comment.getUserId().equals(userId)){
            throw new BizException(ResultCode.COMMENT_NO_PERMISSION);
        }

        // 3. 逻辑删除评论子树
        deleteCommentSubtree(comment, userId, null, OffsetDateTime.now(ZoneOffset.UTC));
    }

    /**
     * 校验文章是否存在并且已发布。
     *
     * @param articleId 文章 ID
     * @return 已发布文章对象
     */
    private Article checkPublishedArticle(Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null){
            throw new BizException(ResultCode.ARTICLE_NOT_FOUND);
        }
        if (article.getStatus() != ArticleStatus.PUBLISHED){
            throw new BizException(ResultCode.ARTICLE_NOT_VISIBLE);
        }
        return article;
    }

    /**
     * 判断单条评论对当前请求者是否可见。
     *
     * @param comment 评论
     * @param currentUserId 当前登录用户 ID；游客为空
     * @return 可见则返回 true，否则返回 false
     */
    private boolean isVisibleComment(Comment comment, Long currentUserId) {
        if (comment.getStatus() == CommentStatus.APPROVED){
            return true;
        }
        return currentUserId != null
                && comment.getUserId().equals(currentUserId)
                && (comment.getStatus() == CommentStatus.PENDING || comment.getStatus() == CommentStatus.REJECTED);
    }

    /**
     * 添加前台评论可见性查询条件。
     *
     * @param wrapper 查询条件
     * @param currentUserId 当前登录用户 ID；游客为空
     */
    private void addVisibleCommentCondition(LambdaQueryWrapper<Comment> wrapper, Long currentUserId) {
        // 游客只看 APPROVED；登录用户还可以看自己的 PENDING / REJECTED。
        wrapper.and(q -> {
            q.eq(Comment::getStatus, CommentStatus.APPROVED);
            if (currentUserId != null){
                q.or(or -> or
                        .eq(Comment::getUserId, currentUserId)
                        .in(Comment::getStatus, List.of(CommentStatus.PENDING, CommentStatus.REJECTED))
                );
            }
        });
    }

    /**
     * 添加回复游标查询条件。
     *
     * @param wrapper 查询条件
     * @param cursor 回复游标；首次请求为空
     */
    private void addReplyCursorCondition(LambdaQueryWrapper<Comment> wrapper, CommentReplyCursorBO cursor) {
        if (cursor == null){
            return;
        }
        // 回复按 createdAt ASC, id ASC 排序，所以“下一页”就是：
        // 1. 创建时间更晚；或者
        // 2. 创建时间相同，但 id 更大。
        wrapper.and(q -> q
                .gt(Comment::getCreatedAt, cursor.getCreatedAt())
                .or(or -> or
                        .eq(Comment::getCreatedAt, cursor.getCreatedAt())
                        .gt(Comment::getId, cursor.getId())
                )
        );
    }

    /**
     * 解析回复分页游标。
     *
     * @param cursor 游标字符串
     * @return 游标对象；首次请求为空
     */
    private CommentReplyCursorBO parseReplyCursor(String cursor) {
        if (!StringUtils.hasText(cursor)){
            return null;
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\|", 2);
            if (parts.length != 2){
                throw new IllegalArgumentException("invalid cursor");
            }
            return CommentReplyCursorBO.builder()
                    .createdAt(OffsetDateTime.parse(parts[0]))
                    .id(Long.valueOf(parts[1]))
                    .build();
        } catch (RuntimeException ex) {
            throw new BizException(ResultCode.PARAM_INVALID);
        }
    }

    /**
     * 生成回复分页游标。
     *
     * @param comment 当前页最后一条评论
     * @return 游标字符串
     */
    private String buildReplyCursor(Comment comment) {
        // createdAt 负责时间顺序，id 负责同一时间下的稳定顺序。
        String raw = comment.getCreatedAt() + "|" + comment.getId();
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 批量查询用户并转换为 Map。
     *
     * @param userIds 用户 ID 列表
     * @return 用户 Map
     */
    private Map<Long, User> getUserMap(List<Long> userIds) {
        List<Long> ids = userIds.stream().filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty()){
            return Map.of();
        }
        return userMapper.selectByIds(ids).stream().collect(Collectors.toMap(User::getId, u -> u));
    }

    /**
     * 查询顶层评论下当前请求者可见的回复数量。
     *
     * @param rootId 顶层评论 ID
     * @param currentUserId 当前登录用户 ID；游客为空
     * @return 可见回复数量
     */
    private long countVisibleReplies(Long rootId, Long currentUserId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<Comment>()
                .eq(Comment::getRootId, rootId);
        addVisibleCommentCondition(wrapper, currentUserId);
        return commentMapper.selectCount(wrapper);
    }

    /**
     * 封装顶层评论 VO 列表。
     *
     * @param comments 评论列表
     * @param currentUserId 当前登录用户 ID；游客为空
     * @return 顶层评论 VO 列表
     */
    private List<PublicCommentItemVO> buildPublicCommentItemVOList(List<Comment> comments, Long currentUserId) {
        List<Long> userIds = comments.stream().map(Comment::getUserId).toList();
        Map<Long, User> userMap = getUserMap(userIds);

        List<PublicCommentItemVO> res = new ArrayList<>();
        comments.forEach(c -> {
            User author = userMap.get(c.getUserId());
            boolean isMine = currentUserId != null && c.getUserId().equals(currentUserId);

            PublicCommentItemVO vo = BeanUtil.copyProperties(c, PublicCommentItemVO.class);
            vo.setAuthor(author == null ? null : BeanUtil.copyProperties(author, CommentAuthorVO.class));
            vo.setReplyCount(countVisibleReplies(c.getId(), currentUserId));
            vo.setIsMine(isMine);
            vo.setModerationReason(isMine && c.getStatus() == CommentStatus.REJECTED ? c.getModerationReason() : null);
            res.add(vo);
        });
        return res;
    }

    /**
     * 封装回复 VO 列表。
     *
     * @param comments 回复列表
     * @param currentUserId 当前登录用户 ID；游客为空
     * @return 回复 VO 列表
     */
    private List<CommentReplyItemVO> buildReplyItemVOList(List<Comment> comments, Long currentUserId) {
        if (comments.isEmpty()){
            return List.of();
        }

        List<Long> userIds = new ArrayList<>(comments.stream().map(Comment::getUserId).toList());
        List<Long> parentIds = comments.stream().map(Comment::getParentId).filter(Objects::nonNull).distinct().toList();
        Map<Long, Comment> parentMap = parentIds.isEmpty()
                ? Map.of()
                : commentMapper.selectByIds(parentIds).stream().collect(Collectors.toMap(Comment::getId, c -> c));
        userIds.addAll(parentMap.values().stream().map(Comment::getUserId).toList());
        Map<Long, User> userMap = getUserMap(userIds);

        List<CommentReplyItemVO> res = new ArrayList<>();
        comments.forEach(c -> {
            Comment parent = parentMap.get(c.getParentId());
            User author = userMap.get(c.getUserId());
            User replyToUser = parent == null ? null : userMap.get(parent.getUserId());
            boolean isMine = currentUserId != null && c.getUserId().equals(currentUserId);

            CommentReplyItemVO vo = BeanUtil.copyProperties(c, CommentReplyItemVO.class);
            vo.setAuthor(author == null ? null : BeanUtil.copyProperties(author, CommentAuthorVO.class));
            vo.setReplyToUser(replyToUser == null ? null : BeanUtil.copyProperties(replyToUser, CommentAuthorVO.class));
            vo.setReplyCount(0L);
            vo.setIsMine(isMine);
            vo.setModerationReason(isMine && c.getStatus() == CommentStatus.REJECTED ? c.getModerationReason() : null);
            res.add(vo);
        });
        return res;
    }

    /**
     * 使用 Redis 原子限流，限制同一用户对同一文章 10 秒内只能成功创建一条评论或回复。
     *
     * @param userId 用户 ID
     * @param articleId 文章 ID
     */
    private void checkCommentRateLimit(Long userId, Long articleId) {
        String key = COMMENT_RATE_LIMIT_KEY_PREFIX + ":user:" + userId + ":article:" + articleId;
        Boolean success = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", COMMENT_RATE_LIMIT_DURATION);
        if (!Boolean.TRUE.equals(success)){
            throw new BizException(ResultCode.COMMENT_RATE_LIMITED);
        }
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

    /**
     * 逻辑删除指定评论及其所有子孙回复，并同步扣减文章已通过评论数。
     *
     * @param comment 待删除的评论
     * @param userId 执行删除操作的管理员用户 ID
     * @param reason 删除原因
     * @param now 删除时间
     */
    private void deleteCommentSubtree(Comment comment, Long userId, String reason, OffsetDateTime now) {
        // 递归 CTE 一次查出目标评论及其全部后代，避免按层反复查询数据库。
        List<Long> commentIds = commentMapper.selectCommentSubtreeIds(comment.getId());
        long approvedCount = commentMapper.countApprovedCommentSubtree(comment.getId());

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
}
