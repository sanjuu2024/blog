package com.ccsanjuu.blog.modules.seo.controller;

import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.modules.seo.model.vo.SeoMetadataVO;
import com.ccsanjuu.blog.modules.seo.service.SeoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/seo")
@RequiredArgsConstructor
@Tag(name = "公开页面 SEO")
public class SeoController {

    private final SeoService seoService;

    /**
     * 获取 SPA 导航使用的公开页面元信息。
     *
     * @param path 公开页面规范路径
     * @return 页面元信息
     */
    @GetMapping
    @Operation(description = "获取公开页面 SEO 元信息")
    public ResponseEntity<Result<SeoMetadataVO>> getMetadata(@RequestParam String path) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Result.success(seoService.getMetadata(path)));
    }
}
