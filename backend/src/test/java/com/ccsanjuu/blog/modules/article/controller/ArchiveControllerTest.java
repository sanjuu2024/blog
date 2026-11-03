package com.ccsanjuu.blog.modules.article.controller;

import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.common.exception.GlobalExceptionHandler;
import com.ccsanjuu.blog.modules.article.service.ArchiveService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ArchiveControllerTest {

    @Mock
    private ArchiveService archiveService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ArchiveController(archiveService))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void returnsEmptyYearsAndBindsArchivePagination() throws Exception {
        when(archiveService.getYears("America/Los_Angeles")).thenReturn(List.of());
        when(archiveService.getArticles(any())).thenReturn(PageResult.of(0, 2, 10, List.of()));

        mockMvc.perform(get("/archives/years").param("timeZone", "America/Los_Angeles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
        mockMvc.perform(get("/archives/articles")
                        .param("timeZone", "America/Los_Angeles")
                        .param("pageNum", "2")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records").isEmpty());

        verify(archiveService).getYears("America/Los_Angeles");
        verify(archiveService).getArticles(argThat(query -> query.getYear() == 2026
                && query.getPageNum() == 2 && "America/Los_Angeles".equals(query.getTimeZone())));
    }

    @ParameterizedTest
    @CsvSource({
            "year,0,年份必须大于等于 1",
            "year,9999,年份不能超过 9998",
            "categoryId,-1,分类 ID 必须大于 0",
            "tagId,0,标签 ID 必须大于 0",
            "pageNum,'',页码不能为空",
            "pageNum,0,页码必须大于等于 1",
            "pageSize,'',每页条数不能为空",
            "pageSize,0,每页条数必须大于等于 1",
            "pageSize,21,每页条数不能超过 20"
    })
    void rejectsInvalidParametersWithChineseMessages(String name, String value, String message) throws Exception {
        mockMvc.perform(get("/archives/articles").param("timeZone", "UTC").param(name, value))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(199001))
                .andExpect(jsonPath("$.data[0].field").value(name))
                .andExpect(jsonPath("$.data[0].message").value(message));

        verifyNoInteractions(archiveService);
    }

    @Test
    void rejectsMalformedYear() throws Exception {
        mockMvc.perform(get("/archives/articles").param("timeZone", "UTC").param("year", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(199001));

        verifyNoInteractions(archiveService);
    }

    @ParameterizedTest
    @CsvSource({
            "year,2026,categoryId,1",
            "year,2026,tagId,1",
            "categoryId,1,tagId,2"
    })
    void rejectsCombinedArchiveDimensions(String firstName, String firstValue, String secondName, String secondValue)
            throws Exception {
        mockMvc.perform(get("/archives/articles")
                        .param("timeZone", "UTC")
                        .param(firstName, firstValue)
                        .param(secondName, secondValue))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(199001))
                .andExpect(jsonPath("$.data[0].message").value("年份、分类和标签最多选择一个"));

        verifyNoInteractions(archiveService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/archives/years", "/archives/articles"})
    void rejectsMissingTimeZone(String path) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(199001))
                .andExpect(jsonPath("$.data[0].message").value("浏览器时区不能为空"));

        verifyNoInteractions(archiveService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "Invalid/Zone", "+08:00", "Asia/Shanghai'; SELECT 1 --"})
    void rejectsInvalidTimeZonesBeforeCallingService(String timeZone) throws Exception {
        for (String path : List.of("/archives/years", "/archives/articles")) {
            mockMvc.perform(get(path).param("timeZone", timeZone))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(199001));
        }

        verifyNoInteractions(archiveService);
    }

    @Test
    void rejectsTimeZoneLongerThanOneHundredCharacters() throws Exception {
        for (String path : List.of("/archives/years", "/archives/articles")) {
            mockMvc.perform(get(path).param("timeZone", "A".repeat(101)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(199001));
        }

        verifyNoInteractions(archiveService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"UTC", "Asia/Shanghai", "America/Los_Angeles", "Europe/London", "Etc/GMT+8"})
    void acceptsNamedBrowserTimeZones(String timeZone) throws Exception {
        when(archiveService.getYears(timeZone)).thenReturn(List.of());

        mockMvc.perform(get("/archives/years").param("timeZone", timeZone))
                .andExpect(status().isOk());

        verify(archiveService).getYears(timeZone);
    }
}
