package com.ccsanjuu.blog.modules.seo.controller;

import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.seo.service.SeoService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;

/** 网站根路径的公开文档接口，不使用 REST API 的版本前缀。 */
@Controller
@RequiredArgsConstructor
public class PublicHtmlController {

    private static final MediaType HTML_UTF8 = new MediaType("text", "html", StandardCharsets.UTF_8);

    private final SeoService seoService;

    /**
     * 返回包含公开元信息的入口 HTML，正文统一由 Vue 渲染，浏览器与爬虫使用相同内容。
     *
     * @param request 当前请求
     * @return 首屏 HTML
     */
    @GetMapping(value = {"/", "/articles", "/about", "/messages", "/articles/{articleId}"},
            produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getHtml(HttpServletRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .contentType(HTML_UTF8).body(seoService.renderHtml(request.getRequestURI()));
    }

    /**
     * 获取公开站点地图。
     *
     * @return XML 文档
     */
    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> getSitemap() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .contentType(new MediaType("application", "xml", StandardCharsets.UTF_8)).body(seoService.getSitemap());
    }

    /**
     * 获取爬虫访问规则。
     *
     * @return robots 文本
     */
    @GetMapping(value = "/robots.txt", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> getRobots() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .contentType(new MediaType("text", "plain", StandardCharsets.UTF_8)).body(seoService.getRobots());
    }

    /**
     * 文章不可公开时返回真正的 404 HTML，避免全局 JSON 错误或 SPA 的软 404。
     *
     * @param exception 公开内容查询失败
     * @return 不可收录的错误页
     */
    @ExceptionHandler(BizException.class)
    public ResponseEntity<String> handleNotFound(BizException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).cacheControl(CacheControl.noStore())
                .contentType(HTML_UTF8).body("<!doctype html><html lang=\"zh-CN\"><head>"
                        + "<meta charset=\"UTF-8\"><meta name=\"robots\" content=\"noindex\">"
                        + "<title>页面不存在</title></head><body><h1>页面不存在</h1><a href=\"/\">返回首页</a></body></html>");
    }

    /**
     * 模板无法读取时返回 503，不输出内部路径和异常详情。
     *
     * @param exception 模板读取失败
     * @return 不可缓存的暂时不可用响应
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<String> handleUnavailable(ResponseStatusException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).cacheControl(CacheControl.noStore())
                .contentType(HTML_UTF8).body("<!doctype html><html lang=\"zh-CN\"><head>"
                        + "<meta charset=\"UTF-8\"><meta name=\"robots\" content=\"noindex\">"
                        + "<title>页面暂时不可用</title></head><body><h1>页面暂时不可用，请稍后重试</h1></body></html>");
    }
}
