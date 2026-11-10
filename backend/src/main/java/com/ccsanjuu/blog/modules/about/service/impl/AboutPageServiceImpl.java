package com.ccsanjuu.blog.modules.about.service.impl;

import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.article.support.ArticleContentRenderer;
import com.ccsanjuu.blog.modules.about.mapper.AboutPageMapper;
import com.ccsanjuu.blog.modules.about.model.dto.UpdateAboutPageRequestDTO;
import com.ccsanjuu.blog.modules.about.model.entity.AboutPage;
import com.ccsanjuu.blog.modules.about.model.vo.AboutPageEditorVO;
import com.ccsanjuu.blog.modules.about.model.vo.AdminAboutPageVO;
import com.ccsanjuu.blog.modules.about.model.vo.PublicAboutPageVO;
import com.ccsanjuu.blog.modules.about.service.AboutPageService;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AboutPageServiceImpl implements AboutPageService {

    private static final Long ABOUT_PAGE_ID = 1L;
    private static final String EMPTY_CONTENT = "暂无内容";

    private final AboutPageMapper aboutPageMapper;
    private final UserMapper userMapper;
    private final ArticleContentRenderer articleContentRenderer;

    @Override
    @Transactional(readOnly = true)
    public PublicAboutPageVO getPublicAboutPage() {
        AboutPage aboutPage = aboutPageMapper.selectById(ABOUT_PAGE_ID);
        if (aboutPage == null) {
            return PublicAboutPageVO.builder()
                    .exists(false)
                    .contentHtml(articleContentRenderer.convertMarkdownToHtml(EMPTY_CONTENT))
                    .contentText(EMPTY_CONTENT)
                    .build();
        }
        return PublicAboutPageVO.builder()
                .exists(true)
                .contentHtml(aboutPage.getContentHtml())
                .contentText(aboutPage.getContentText())
                .updatedAt(aboutPage.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminAboutPageVO getAdminAboutPage() {
        AboutPage aboutPage = aboutPageMapper.selectById(ABOUT_PAGE_ID);
        if (aboutPage == null) {
            return AdminAboutPageVO.builder()
                    .exists(false)
                    .contentMd("")
                    .contentHtml(articleContentRenderer.convertMarkdownToHtml(EMPTY_CONTENT))
                    .contentText(EMPTY_CONTENT)
                    .build();
        }
        return toAdminAboutPageVO(aboutPage);
    }

    @Override
    @Transactional
    public AdminAboutPageVO upsertAboutPage(Long adminId, UpdateAboutPageRequestDTO requestDTO) {
        String contentMd = requestDTO.getContentMd().strip();
        AboutPage aboutPage = aboutPageMapper.selectById(ABOUT_PAGE_ID);
        if (aboutPage != null && contentMd.equals(aboutPage.getContentMd())) {
            return toAdminAboutPageVO(aboutPage);
        }

        String contentHtml = articleContentRenderer.convertMarkdownToHtml(contentMd);
        String contentText = articleContentRenderer.convertToText(contentMd);
        if (aboutPage == null) {
            aboutPage = AboutPage.builder()
                    .id(ABOUT_PAGE_ID)
                    .contentMd(contentMd)
                    .contentHtml(contentHtml)
                    .contentText(contentText)
                    .updatedBy(adminId)
                    .build();
            aboutPageMapper.insert(aboutPage);
        } else {
            AboutPage updateAboutPage = AboutPage.builder()
                    .id(ABOUT_PAGE_ID)
                    .contentMd(contentMd)
                    .contentHtml(contentHtml)
                    .contentText(contentText)
                    .updatedBy(adminId)
                    .build();
            aboutPageMapper.updateById(updateAboutPage);
        }
        aboutPage = aboutPageMapper.selectById(ABOUT_PAGE_ID);
        return toAdminAboutPageVO(aboutPage);
    }

    private AdminAboutPageVO toAdminAboutPageVO(AboutPage aboutPage) {
        User editor = userMapper.selectById(aboutPage.getUpdatedBy());
        if (editor == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        return AdminAboutPageVO.builder()
                .exists(true)
                .contentMd(aboutPage.getContentMd())
                .contentHtml(aboutPage.getContentHtml())
                .contentText(aboutPage.getContentText())
                .updatedBy(AboutPageEditorVO.builder()
                        .id(editor.getId())
                        .username(editor.getUsername())
                        .build())
                .updatedAt(aboutPage.getUpdatedAt())
                .build();
    }
}
