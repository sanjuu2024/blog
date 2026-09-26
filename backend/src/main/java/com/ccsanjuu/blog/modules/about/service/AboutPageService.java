package com.ccsanjuu.blog.modules.about.service;

import com.ccsanjuu.blog.modules.about.model.dto.UpdateAboutPageRequestDTO;
import com.ccsanjuu.blog.modules.about.model.vo.AdminAboutPageVO;
import com.ccsanjuu.blog.modules.about.model.vo.PublicAboutPageVO;

public interface AboutPageService {

    PublicAboutPageVO getPublicAboutPage();

    AdminAboutPageVO getAdminAboutPage();

    AdminAboutPageVO upsertAboutPage(Long adminId, UpdateAboutPageRequestDTO requestDTO);
}
