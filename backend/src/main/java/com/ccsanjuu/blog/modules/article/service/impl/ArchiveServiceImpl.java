package com.ccsanjuu.blog.modules.article.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.article.mapper.ArticleMapper;
import com.ccsanjuu.blog.modules.article.model.dto.ArchiveQueryDTO;
import com.ccsanjuu.blog.modules.article.model.vo.ArchiveArticleVO;
import com.ccsanjuu.blog.modules.article.model.vo.ArchiveYearVO;
import com.ccsanjuu.blog.modules.article.service.ArchiveService;
import com.ccsanjuu.blog.modules.category.mapper.CategoryMapper;
import com.ccsanjuu.blog.modules.category.model.enums.CategoryStatus;
import com.ccsanjuu.blog.modules.tag.mapper.TagMapper;
import com.ccsanjuu.blog.modules.tag.model.enums.TagStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ArchiveServiceImpl implements ArchiveService {

    private final ArticleMapper articleMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;

    /**
     * 一次聚合全部公开文章的年份与篇数。
     *
     * @param timeZone 已校验的浏览器 IANA 命名时区
     * @return 按年份倒序的完整汇总
     */
    @Override
    public List<ArchiveYearVO> getYears(String timeZone) {
        return articleMapper.selectArchiveYears(timeZone);
    }

    /**
     * 校验归档筛选资源后，联表获取公开文章和完整分类路径。
     *
     * @param query 归档条件和分页参数
     * @return 按首次发布时间倒序的归档分页
     */
    @Override
    public PageResult<ArchiveArticleVO> getArticles(ArchiveQueryDTO query) {
        if (query.getCategoryId() != null) {
            var category = categoryMapper.selectById(query.getCategoryId());
            if (category == null) {
                throw new BizException(ResultCode.ARTICLE_CATEGORY_NOT_FOUND);
            }
            if (category.getStatus() != CategoryStatus.ENABLED) {
                throw new BizException(ResultCode.ARTICLE_CATEGORY_DISABLED);
            }
            if (category.getLevel() == 2) {
                var parent = categoryMapper.selectById(category.getParentId());
                if (parent == null) {
                    throw new BizException(ResultCode.ARTICLE_CATEGORY_NOT_FOUND);
                }
                if (parent.getStatus() != CategoryStatus.ENABLED) {
                    throw new BizException(ResultCode.ARTICLE_CATEGORY_DISABLED);
                }
            }
        }
        if (query.getTagId() != null) {
            var tag = tagMapper.selectById(query.getTagId());
            if (tag == null) {
                throw new BizException(ResultCode.ARTICLE_TAG_NOT_FOUND);
            }
            if (tag.getStatus() != TagStatus.ENABLED) {
                throw new BizException(ResultCode.ARTICLE_TAG_DISABLED);
            }
        }

        OffsetDateTime start = null;
        OffsetDateTime end = null;
        if (query.getYear() != null) {
            // 与浏览器当地日期采用同一时区；两端分别应用当年的时区规则，避免夏令时变化影响边界。
            ZoneId timeZone = ZoneId.of(query.getTimeZone());
            LocalDate yearStart = LocalDate.of(query.getYear(), 1, 1);
            start = yearStart.atStartOfDay(timeZone).toOffsetDateTime();
            end = yearStart.plusYears(1).atStartOfDay(timeZone).toOffsetDateTime();
        }
        Page<ArchiveArticleVO> page = Page.of(query.getPageNum(), query.getPageSize());
        articleMapper.selectArchivePage(page, start, end, query.getCategoryId(), query.getTagId());
        return PageResult.of(page);
    }
}
