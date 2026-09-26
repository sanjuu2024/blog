package com.ccsanjuu.blog.modules.privacy.service;

import com.ccsanjuu.blog.modules.privacy.model.vo.PrivacyPolicyVO;

public interface PrivacyPolicyService {

    PrivacyPolicyVO getPrivacyPolicy();

    boolean compareTo(String version);
}
