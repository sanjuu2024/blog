package com.ccsanjuu.blog.modules.about.controller;

import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.modules.about.model.vo.PublicAboutPageVO;
import com.ccsanjuu.blog.modules.about.service.AboutPageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/about")
@RequiredArgsConstructor
@Tag(name = "站点内容接口")
public class AboutPageController {

    private final AboutPageService aboutPageService;

    /**
     * 前台获取关于页内容
     *
     * @return
     */
    @GetMapping
    @Operation(description = "获取关于页")
    public Result<PublicAboutPageVO> getAboutPage() {
        return Result.success(aboutPageService.getPublicAboutPage());
    }
}
