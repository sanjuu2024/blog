package com.ccsanjuu.blog.modules.article.controller;

import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.modules.article.model.dto.ArchiveQueryDTO;
import com.ccsanjuu.blog.modules.article.model.dto.ArchiveTimeZoneQueryDTO;
import com.ccsanjuu.blog.modules.article.model.vo.ArchiveArticleVO;
import com.ccsanjuu.blog.modules.article.model.vo.ArchiveYearVO;
import com.ccsanjuu.blog.modules.article.service.ArchiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/archives")
@RequiredArgsConstructor
@Tag(name = "前台归档接口")
public class ArchiveController {

    private final ArchiveService archiveService;

    /**
     * 获取年份及完整文章篇数。
     *
     * @param query 浏览器时区
     * @return 公开文章年份汇总
     */
    @GetMapping("/years")
    @Operation(description = "获取公开文章年份及完整篇数")
    public Result<List<ArchiveYearVO>> getYears(@Valid @ModelAttribute ArchiveTimeZoneQueryDTO query) {
        return Result.success(archiveService.getYears(query.getTimeZone()));
    }

    /**
     * 分页获取归档文章，不增加浏览计数。
     *
     * @param query 归档筛选与分页
     * @return 紧凑文章分页
     */
    @GetMapping("/articles")
    @Operation(description = "分页获取紧凑归档文章")
    public Result<PageResult<ArchiveArticleVO>> getArticles(@Valid @ModelAttribute ArchiveQueryDTO query) {
        return Result.success(archiveService.getArticles(query));
    }
}
