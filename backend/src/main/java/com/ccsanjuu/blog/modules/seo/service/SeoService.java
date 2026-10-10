package com.ccsanjuu.blog.modules.seo.service;

import com.ccsanjuu.blog.modules.seo.model.vo.SeoMetadataVO;

public interface SeoService {

    /**
     * 获取公开页面元信息，不生成用户状态或记录浏览。
     *
     * @param path 页面规范路径
     * @return 页面元信息
     */
    SeoMetadataVO getMetadata(String path);

    /**
     * 根据前端入口模板生成可直接抓取的 HTML。
     *
     * @param path 页面规范路径
     * @return 含元信息及空 Vue 挂载容器的 HTML
     */
    String renderHtml(String path);

    /**
     * 生成公开页面站点地图。
     *
     * @return sitemap XML
     */
    String getSitemap();

    /**
     * 获取爬虫规则及站点地图地址。
     *
     * @return robots 文本
     */
    String getRobots();
}
