package com.ccsanjuu.blog.modules.article.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.article.mapper.ArticleMapper;
import com.ccsanjuu.blog.modules.article.model.dto.ArchiveQueryDTO;
import com.ccsanjuu.blog.modules.article.model.vo.ArchiveArticleVO;
import com.ccsanjuu.blog.modules.category.mapper.CategoryMapper;
import com.ccsanjuu.blog.modules.category.model.entity.Category;
import com.ccsanjuu.blog.modules.category.model.enums.CategoryStatus;
import com.ccsanjuu.blog.modules.tag.mapper.TagMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArchiveServiceImplTest {

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private TagMapper tagMapper;

    @InjectMocks
    private ArchiveServiceImpl archiveService;

    @ParameterizedTest
    @CsvSource({
            "Asia/Shanghai,2026,2026-01-01T00:00:00+08:00,2027-01-01T00:00:00+08:00",
            "America/Los_Angeles,2026,2026-01-01T00:00:00-08:00,2027-01-01T00:00:00-08:00",
            "Europe/London,2026,2026-01-01T00:00:00Z,2027-01-01T00:00:00Z",
            "Asia/Kathmandu,2026,2026-01-01T00:00:00+05:45,2027-01-01T00:00:00+05:45",
            "America/Sao_Paulo,2019,2019-01-01T00:00:00-02:00,2020-01-01T00:00:00-03:00"
    })
    void yearFilterUsesBrowserTimeZoneAndIndependentYearBoundaries(String timeZone, int year, String start, String end) {
        ArchiveQueryDTO query = new ArchiveQueryDTO();
        query.setTimeZone(timeZone);
        query.setYear(year);
        when(articleMapper.selectArchivePage(any(), any(), any(), isNull(), isNull())).thenAnswer(call -> {
            Page<ArchiveArticleVO> page = call.getArgument(0);
            page.setTotal(12);
            page.setRecords(List.of(new ArchiveArticleVO()));
            return page;
        });

        var result = archiveService.getArticles(query);

        assertEquals(12, result.getTotal());
        assertTrue(result.isHasNext());
        verify(articleMapper).selectArchivePage(
                any(),
                eq(OffsetDateTime.parse(start)),
                eq(OffsetDateTime.parse(end)),
                isNull(),
                isNull()
        );
        verifyNoInteractions(categoryMapper, tagMapper);
        verifyNoMoreInteractions(articleMapper);
    }

    @ParameterizedTest
    @ValueSource(strings = {"UTC", "Asia/Shanghai", "America/Los_Angeles"})
    void yearsUseOneAggregateForTheBrowserTimeZone(String timeZone) {
        when(articleMapper.selectArchiveYears(timeZone)).thenReturn(List.of());

        assertTrue(archiveService.getYears(timeZone).isEmpty());

        verify(articleMapper).selectArchiveYears(timeZone);
        verifyNoMoreInteractions(articleMapper);
    }

    @Test
    void disabledParentRejectsCategoryQuery() {
        ArchiveQueryDTO query = new ArchiveQueryDTO();
        query.setCategoryId(2L);
        when(categoryMapper.selectById(2L)).thenReturn(Category.builder()
                .level(2)
                .parentId(1L)
                .status(CategoryStatus.ENABLED)
                .build());
        when(categoryMapper.selectById(1L)).thenReturn(Category.builder().status(CategoryStatus.DISABLED).build());

        BizException exception = assertThrows(BizException.class, () -> archiveService.getArticles(query));

        assertEquals(ResultCode.ARTICLE_CATEGORY_DISABLED, exception.getResultCode());
        verifyNoInteractions(articleMapper);
    }

    @Test
    void missingTagRejectsTagQuery() {
        ArchiveQueryDTO query = new ArchiveQueryDTO();
        query.setTagId(3L);

        BizException exception = assertThrows(BizException.class, () -> archiveService.getArticles(query));

        assertEquals(ResultCode.ARTICLE_TAG_NOT_FOUND, exception.getResultCode());
        verifyNoInteractions(articleMapper);
    }
}
