package com.ccsanjuu.blog.modules.privacy.controller;

import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.modules.privacy.model.vo.PrivacyPolicyVO;
import com.ccsanjuu.blog.modules.privacy.service.PrivacyPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/privacy-policy")
@RequiredArgsConstructor
@Tag(name = "站点内容接口")
public class PrivacyPolicyController {

    private final PrivacyPolicyService privacyPolicyService;

    @GetMapping
    @Operation(description = "获取当前隐私政策")
    public ResponseEntity<Result<PrivacyPolicyVO>> getPrivacyPolicy(
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch
    ) {
        PrivacyPolicyVO privacyPolicy = privacyPolicyService.getPrivacyPolicy();
        String etag = '"' + privacyPolicy.getVersion() + '"';
        if (etag.equals(ifNoneMatch)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                    .eTag(privacyPolicy.getVersion())
                    .cacheControl(CacheControl.noCache())
                    .build();
        }
        return ResponseEntity.ok()
                .eTag(privacyPolicy.getVersion())
                .cacheControl(CacheControl.noCache())
                .body(Result.success(privacyPolicy));
    }
}
