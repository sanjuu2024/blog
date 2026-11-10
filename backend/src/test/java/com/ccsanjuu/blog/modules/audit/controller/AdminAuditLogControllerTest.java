package com.ccsanjuu.blog.modules.audit.controller;

import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.common.exception.GlobalExceptionHandler;
import com.ccsanjuu.blog.config.WebMvcConfig;
import com.ccsanjuu.blog.modules.article.mapper.ArticleMapper;
import com.ccsanjuu.blog.modules.article.mapper.ArticleTagMapper;
import com.ccsanjuu.blog.modules.audit.mapper.AdminAuditLogMapper;
import com.ccsanjuu.blog.modules.audit.model.dto.AdminAuditLogQueryDTO;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditAction;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditResourceType;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditResult;
import com.ccsanjuu.blog.modules.audit.model.vo.AdminAuditLogItemVO;
import com.ccsanjuu.blog.modules.audit.service.AdminAuditLogService;
import com.ccsanjuu.blog.modules.auth.mapper.AuthMapper;
import com.ccsanjuu.blog.modules.category.mapper.CategoryMapper;
import com.ccsanjuu.blog.modules.comment.mapper.CommentMapper;
import com.ccsanjuu.blog.modules.message.mapper.MessageMapper;
import com.ccsanjuu.blog.modules.tag.mapper.TagMapper;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminAuditLogController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, WebMvcConfig.class})
class AdminAuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminAuditLogService adminAuditLogService;

    @MockitoBean
    private AdminAuditLogMapper adminAuditLogMapper;

    @MockitoBean
    private ArticleMapper articleMapper;

    @MockitoBean
    private ArticleTagMapper articleTagMapper;

    @MockitoBean
    private AuthMapper authMapper;

    @MockitoBean
    private CategoryMapper categoryMapper;

    @MockitoBean
    private CommentMapper commentMapper;

    @MockitoBean
    private MessageMapper messageMapper;

    @MockitoBean
    private TagMapper tagMapper;

    @MockitoBean
    private UserMapper userMapper;

    @Test
    void shouldBindAuditFiltersAndReturnPage() throws Exception {
        OffsetDateTime from = OffsetDateTime.parse("2026-09-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-09-30T23:59:59Z");
        when(adminAuditLogService.getAuditLogList(any(AdminAuditLogQueryDTO.class)))
                .thenReturn(PageResult.of(1L, 1L, 10L, List.of(AdminAuditLogItemVO.builder()
                        .id(1L)
                        .operatorId(10001L)
                        .resourceType(AdminAuditResourceType.COMMENT)
                        .action(AdminAuditAction.MODERATE)
                        .result(AdminAuditResult.SUCCESS)
                        .build())));

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .queryParam("operatorId", "10001")
                        .queryParam("resourceType", "COMMENT")
                        .queryParam("resourceId", "60001")
                        .queryParam("action", "MODERATE")
                        .queryParam("result", "SUCCESS")
                        .queryParam("createdAtFrom", from.toString())
                        .queryParam("createdAtTo", to.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.records[0].id").value(1));

        ArgumentCaptor<AdminAuditLogQueryDTO> captor = ArgumentCaptor.forClass(AdminAuditLogQueryDTO.class);
        verify(adminAuditLogService).getAuditLogList(captor.capture());
        assertEquals(10001L, captor.getValue().getOperatorId());
        assertEquals(AdminAuditResourceType.COMMENT, captor.getValue().getResourceType());
        assertEquals("60001", captor.getValue().getResourceId());
        assertEquals(AdminAuditAction.MODERATE, captor.getValue().getAction());
        assertEquals(AdminAuditResult.SUCCESS, captor.getValue().getResult());
        assertEquals(from, captor.getValue().getCreatedAtFrom());
        assertEquals(to, captor.getValue().getCreatedAtTo());
    }

    @Test
    void shouldRejectInvalidOperatorId() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit-logs").queryParam("operatorId", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(199001));
    }
}
