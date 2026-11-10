package com.ccsanjuu.blog.modules.notification.controller;

import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.common.exception.GlobalExceptionHandler;
import com.ccsanjuu.blog.modules.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminNotificationControllerTest {

    @Test
    void shouldRegisterAdminMessageRoutesWithoutDuplicateMappings() {
        AdminNotificationController controller = new AdminNotificationController(mock(NotificationService.class));

        assertDoesNotThrow(() -> MockMvcBuilders.standaloneSetup(controller).build());
    }

    @Test
    void shouldReturnSpecificReasonWhenSelectedRecipientWasDeleted() throws Exception {
        NotificationService service = mock(NotificationService.class);
        when(service.updateAdminMessage(eq(70001L), any())).thenThrow(
                new BizException(ResultCode.PARAM_INVALID, "指定用户已注销，无法接收管理员消息"));

        MockMvcBuilders.standaloneSetup(new AdminNotificationController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build()
                .perform(put("/admin/notifications/70001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"targetScope":"SELECTED_USERS","userId":10002,"title":"系统消息","content":"正文"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(199001))
                .andExpect(jsonPath("$.message").value("指定用户已注销，无法接收管理员消息"));
    }
}
