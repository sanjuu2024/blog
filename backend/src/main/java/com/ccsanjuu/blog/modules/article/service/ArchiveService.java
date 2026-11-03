package com.ccsanjuu.blog.modules.article.service;

import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.modules.article.model.dto.ArchiveQueryDTO;
import com.ccsanjuu.blog.modules.article.model.vo.ArchiveArticleVO;
import com.ccsanjuu.blog.modules.article.model.vo.ArchiveYearVO;

import java.util.List;

public interface ArchiveService {

    /**
     * 一次聚合全部公开文章的年份与篇数。
     *
     * @param timeZone 已校验的浏览器 IANA 命名时区
     * @return 按年份倒序的完整汇总
     */
    List<ArchiveYearVO> getYears(String timeZone);

    /**
     * 获取按首次发布时间倒序的紧凑归档。
     *
     * @param query 归档条件和分页参数
     * @return 归档分页
     */
    PageResult<ArchiveArticleVO> getArticles(ArchiveQueryDTO query);
}
