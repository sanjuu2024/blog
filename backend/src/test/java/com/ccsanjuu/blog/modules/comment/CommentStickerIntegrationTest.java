package com.ccsanjuu.blog.modules.comment;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccsanjuu.blog.modules.article.mapper.ArticleMapper;
import com.ccsanjuu.blog.modules.article.model.entity.Article;
import com.ccsanjuu.blog.modules.comment.mapper.CommentMapper;
import com.ccsanjuu.blog.modules.comment.model.dto.AdminCommentQueryDTO;
import com.ccsanjuu.blog.modules.comment.model.dto.CreateCommentRequestDTO;
import com.ccsanjuu.blog.modules.comment.model.dto.PublicCommentQueryDTO;
import com.ccsanjuu.blog.modules.comment.model.entity.Comment;
import com.ccsanjuu.blog.modules.comment.model.enums.CommentStatus;
import com.ccsanjuu.blog.modules.comment.service.CommentService;
import com.ccsanjuu.blog.modules.comment.service.CommentStickerService;
import com.ccsanjuu.blog.modules.notification.model.dto.NotificationQueryDTO;
import com.ccsanjuu.blog.modules.notification.mapper.NotificationMapper;
import com.ccsanjuu.blog.modules.notification.mapper.NotificationRecipientMapper;
import com.ccsanjuu.blog.modules.notification.model.entity.Notification;
import com.ccsanjuu.blog.modules.notification.model.entity.NotificationRecipient;
import com.ccsanjuu.blog.modules.notification.model.enums.NotificationStatus;
import com.ccsanjuu.blog.modules.notification.model.enums.NotificationTargetScope;
import com.ccsanjuu.blog.modules.notification.model.enums.NotificationType;
import com.ccsanjuu.blog.modules.notification.service.NotificationService;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CommentStickerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Validator validator;

    @Autowired
    private CommentService commentService;

    @Autowired
    private CommentStickerService stickerService;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private NotificationMapper notificationMapper;

    @Autowired
    private NotificationRecipientMapper recipientMapper;

    private String rateKey;

    @AfterEach
    void clearRateLimit() {
        if (rateKey != null) redis.delete(rateKey);
    }

    @Test
    void publicCatalogShouldUseTheSameTokensAsValidation() throws Exception {
        mockMvc.perform(get("/api/v1/comments/stickers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].gridMinWidthRem").value(4.5))
                .andExpect(jsonPath("$.data[1].gridMinWidthRem").doesNotExist())
                .andExpect(jsonPath("$.data[1].items[0].token").value("[sticker:bilibili:doge]"));
        String token = stickerService.getPacks().get(1).items().getFirst().token();
        assertTrue(validator.validate(CreateCommentRequestDTO.builder().content("字".repeat(980) + token.repeat(20)).build()).isEmpty());
        assertFalse(validator.validate(CreateCommentRequestDTO.builder().content(token.repeat(21)).build()).isEmpty());
        assertFalse(validator.validate(CreateCommentRequestDTO.builder().content("字".repeat(1000) + token).build()).isEmpty());
        assertTrue(validator.validate(CreateCommentRequestDTO.builder().content("[sticker:unknown:doge]".repeat(21)).build()).isEmpty());
        assertTrue(validator.validate(CreateCommentRequestDTO.builder().content(" ".repeat(1260) + "字".repeat(1000)).build()).isEmpty());
        assertFalse(validator.validate(CreateCommentRequestDTO.builder().content(" ".repeat(1261) + "字".repeat(1000)).build()).isEmpty());
    }

    @Test
    void storedAndPublicContentShouldKeepTokensWhileAdminAndNotificationUseText() {
        User admin = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, "admin"));
        Article article = articleMapper.selectOne(new LambdaQueryWrapper<Article>()
                .eq(Article::getSlug, "spring-boot-dual-token-login"));
        rateKey = "blog:comment:rate:user:" + admin.getId() + ":article:" + article.getId();
        redis.delete(rateKey);
        String source = "支持[sticker:bilibili:doge]";
        var created = commentService.createComment(article.getId(), admin.getId(),
                CreateCommentRequestDTO.builder().content(source).build());
        assertEquals(CommentStatus.APPROVED, created.getStatus());
        assertEquals(source, commentMapper.selectById(created.getId()).getContent());
        assertEquals(source, commentService.getPublicCommentList(article.getId(), admin.getId(), new PublicCommentQueryDTO())
                .getRecords().stream().filter(item -> item.getId().equals(created.getId())).findFirst().orElseThrow().getContent());
        AdminCommentQueryDTO query = new AdminCommentQueryDTO();
        query.setArticleId(article.getId());
        assertEquals("支持[doge]", commentService.getAdminCommentList(query).getRecords().stream()
                .filter(item -> item.getId().equals(created.getId())).findFirst().orElseThrow().getContent());

        var reply = Comment.builder()
                .articleId(article.getId()).userId(admin.getId()).parentId(created.getId()).rootId(created.getId())
                .content("[sticker:wechat:微笑]").status(CommentStatus.APPROVED).build();
        commentMapper.insert(reply);
        // 在当前回滚事务中构造历史标记快照，核对真实联表查询的原文与回复文字降级。
        var notification = Notification.builder()
                .type(NotificationType.COMMENT_REPLY)
                .targetScope(NotificationTargetScope.SELECTED_USERS)
                .title("回复").content(reply.getContent())
                .status(NotificationStatus.PUBLISHED)
                .sourceType("COMMENT").sourceId(reply.getId()).build();
        notificationMapper.insert(notification);
        recipientMapper.insert(NotificationRecipient.builder()
                .notificationId(notification.getId()).userId(admin.getId()).build());
        var item = notificationService.getUserNotifications(admin.getId(), new NotificationQueryDTO()).getRecords().stream()
                .filter(value -> value.getId().equals(notification.getId())).findFirst().orElseThrow();
        assertEquals("[微笑]", item.getContent());
        assertEquals("支持[doge]", item.getOriginalContent());
    }
}
