package com.ccsanjuu.blog.modules.security.controller;

import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.modules.security.service.SecurityEventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminSecurityEventControllerTest {

    private SecurityEventService securityEventService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        securityEventService = mock(SecurityEventService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new AdminSecurityEventController(securityEventService)).build();
    }

    @Test
    void listShouldPassValidatedFiltersToService() throws Exception {
        when(securityEventService.getSecurityEventList(any())).thenReturn(
                PageResult.of(0, 1, 10, List.of()));

        mockMvc.perform(get("/admin/security-events")
                        .param("eventType", "LOGIN")
                        .param("outcome", "FAILURE")
                        .param("userId", "10001"))
                .andExpect(status().isOk());

        verify(securityEventService).getSecurityEventList(any());
    }
}
