package com.ccsanjuu.blog.modules.about.service.impl;

import com.ccsanjuu.blog.modules.article.support.ArticleContentRenderer;
import com.ccsanjuu.blog.modules.about.mapper.AboutPageMapper;
import com.ccsanjuu.blog.modules.about.model.dto.UpdateAboutPageRequestDTO;
import com.ccsanjuu.blog.modules.about.model.entity.AboutPage;
import com.ccsanjuu.blog.modules.about.model.vo.AdminAboutPageVO;
import com.ccsanjuu.blog.modules.about.model.vo.PublicAboutPageVO;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AboutPageServiceImplTest {

    private static final Long ADMIN_ID = 10001L;

    @Mock
    private AboutPageMapper aboutPageMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ArticleContentRenderer articleContentRenderer;

    private AboutPageServiceImpl aboutPageService;

    @BeforeEach
    void setUp() {
        aboutPageService = new AboutPageServiceImpl(
                aboutPageMapper,
                userMapper,
                articleContentRenderer
        );
    }

    @Test
    void shouldReturnPlaceholderWhenPublicAboutPageDoesNotExist() {
        when(aboutPageMapper.selectById(1L)).thenReturn(null);
        when(articleContentRenderer.convertMarkdownToHtml("暂无内容"))
                .thenReturn("<p>暂无内容</p>");

        PublicAboutPageVO result = aboutPageService.getPublicAboutPage();

        assertFalse(result.getExists());
        assertEquals("<p>暂无内容</p>", result.getContentHtml());
        assertEquals("暂无内容", result.getContentText());
    }

    @Test
    void shouldReturnEmptyEditorWhenAdminAboutPageDoesNotExist() {
        when(aboutPageMapper.selectById(1L)).thenReturn(null);
        when(articleContentRenderer.convertMarkdownToHtml("暂无内容"))
                .thenReturn("<p>暂无内容</p>");

        AdminAboutPageVO result = aboutPageService.getAdminAboutPage();

        assertFalse(result.getExists());
        assertEquals("", result.getContentMd());
        assertEquals("暂无内容", result.getContentText());
    }

    @Test
    void shouldCreateAboutPageWithRenderedContent() {
        OffsetDateTime updatedAt = OffsetDateTime.now();
        when(aboutPageMapper.selectById(1L)).thenReturn(null);
        when(articleContentRenderer.convertMarkdownToHtml("# 关于本站"))
                .thenReturn("<h1>关于本站</h1>");
        when(articleContentRenderer.convertToText("# 关于本站")).thenReturn("关于本站");
        when(aboutPageMapper.insert(any(AboutPage.class))).thenAnswer(invocation -> {
            AboutPage aboutPage = invocation.getArgument(0);
            aboutPage.setUpdatedAt(updatedAt);
            return 1;
        });
        when(userMapper.selectById(ADMIN_ID)).thenReturn(editor());

        AdminAboutPageVO result = aboutPageService.upsertAboutPage(
                ADMIN_ID,
                new UpdateAboutPageRequestDTO("  # 关于本站  ")
        );

        ArgumentCaptor<AboutPage> captor = ArgumentCaptor.forClass(AboutPage.class);
        verify(aboutPageMapper).insert(captor.capture());
        assertEquals(1L, captor.getValue().getId());
        assertEquals("# 关于本站", captor.getValue().getContentMd());
        assertEquals(ADMIN_ID, captor.getValue().getUpdatedBy());
        assertTrue(result.getExists());
        assertEquals("admin", result.getUpdatedBy().getUsername());
        assertEquals(updatedAt, result.getUpdatedAt());
    }

    @Test
    void shouldNotUpdateUnchangedMarkdown() {
        AboutPage aboutPage = aboutPage();
        when(aboutPageMapper.selectById(1L)).thenReturn(aboutPage);
        when(userMapper.selectById(ADMIN_ID)).thenReturn(editor());

        AdminAboutPageVO result = aboutPageService.upsertAboutPage(
                ADMIN_ID,
                new UpdateAboutPageRequestDTO("# 关于本站")
        );

        assertEquals(aboutPage.getUpdatedAt(), result.getUpdatedAt());
        verify(aboutPageMapper, never()).updateById(any(AboutPage.class));
        verifyNoInteractions(articleContentRenderer);
    }

    @Test
    void shouldUpdateChangedMarkdownAndEditor() {
        OffsetDateTime updatedAt = OffsetDateTime.now().plusMinutes(1);
        when(aboutPageMapper.selectById(1L)).thenReturn(aboutPage());
        when(articleContentRenderer.convertMarkdownToHtml("## 新内容"))
                .thenReturn("<h2>新内容</h2>");
        when(articleContentRenderer.convertToText("## 新内容")).thenReturn("新内容");
        when(aboutPageMapper.updateById(any(AboutPage.class))).thenAnswer(invocation -> {
            AboutPage update = invocation.getArgument(0);
            update.setUpdatedAt(updatedAt);
            return 1;
        });
        when(userMapper.selectById(ADMIN_ID)).thenReturn(editor());

        AdminAboutPageVO result = aboutPageService.upsertAboutPage(
                ADMIN_ID,
                new UpdateAboutPageRequestDTO("## 新内容")
        );

        ArgumentCaptor<AboutPage> captor = ArgumentCaptor.forClass(AboutPage.class);
        verify(aboutPageMapper).updateById(captor.capture());
        assertEquals("## 新内容", captor.getValue().getContentMd());
        assertEquals("<h2>新内容</h2>", result.getContentHtml());
        assertEquals(updatedAt, result.getUpdatedAt());
    }

    private AboutPage aboutPage() {
        return AboutPage.builder()
                .id(1L)
                .contentMd("# 关于本站")
                .contentHtml("<h1>关于本站</h1>")
                .contentText("关于本站")
                .updatedBy(ADMIN_ID)
                .updatedAt(OffsetDateTime.now())
                .build();
    }

    private User editor() {
        return User.builder()
                .id(ADMIN_ID)
                .username("admin")
                .build();
    }
}
