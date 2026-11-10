package com.ccsanjuu.blog.modules.about;

import com.ccsanjuu.blog.modules.about.mapper.AboutPageMapper;
import com.ccsanjuu.blog.modules.about.model.dto.UpdateAboutPageRequestDTO;
import com.ccsanjuu.blog.modules.about.model.vo.AdminAboutPageVO;
import com.ccsanjuu.blog.modules.about.model.vo.PublicAboutPageVO;
import com.ccsanjuu.blog.modules.about.service.AboutPageService;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import com.ccsanjuu.blog.modules.user.model.enums.UserRole;
import com.ccsanjuu.blog.modules.user.model.enums.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class AboutPageIntegrationTest {

    @Autowired
    private AboutPageService aboutPageService;

    @Autowired
    private AboutPageMapper aboutPageMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private User admin;

    @BeforeEach
    void setUp() {
        aboutPageMapper.deleteById(1L);
        String username = "about" + Long.toString(System.nanoTime(), 36);
        admin = User.builder()
                .username(username)
                .nickname(username)
                .email(username + "@example.com")
                .passwordHash("integration-test-password")
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .tokenVersion(0L)
                .avatarUrl("")
                .bio("")
                .emailVerified(true)
                .build();
        userMapper.insert(admin);
    }

    @Test
    void shouldCreateReadAndIdempotentlyUpdateAboutPage() {
        PublicAboutPageVO emptyPage = aboutPageService.getPublicAboutPage();
        assertFalse(emptyPage.getExists());
        assertEquals("暂无内容", emptyPage.getContentText());

        UpdateAboutPageRequestDTO request = new UpdateAboutPageRequestDTO("""
                # 关于本站

                欢迎访问。

                <script>alert(1)</script>
                """);
        AdminAboutPageVO saved = aboutPageService.upsertAboutPage(admin.getId(), request);
        AdminAboutPageVO unchanged = aboutPageService.upsertAboutPage(admin.getId(), request);
        PublicAboutPageVO publicPage = aboutPageService.getPublicAboutPage();

        assertTrue(saved.getExists());
        assertEquals(admin.getId(), saved.getUpdatedBy().getId());
        assertEquals(saved.getUpdatedAt(), unchanged.getUpdatedAt());
        assertTrue(publicPage.getContentHtml().contains("<h1>关于本站</h1>"));
        assertFalse(publicPage.getContentHtml().contains("<script>"));
        assertTrue(publicPage.getContentText().contains("欢迎访问"));
    }

    @Test
    void databaseShouldRejectAdditionalAboutPageRows() {
        assertThrows(
                DataIntegrityViolationException.class,
                () -> jdbcTemplate.update(
                        "INSERT INTO blog_about_page "
                                + "(id, content_md, content_html, content_text, updated_by) "
                                + "VALUES (2, '', '', '', ?)",
                        admin.getId()
                )
        );
    }
}
