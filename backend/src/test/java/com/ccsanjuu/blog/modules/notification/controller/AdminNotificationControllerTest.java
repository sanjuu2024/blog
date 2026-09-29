package com.ccsanjuu.blog.modules.notification.controller;

import com.ccsanjuu.blog.modules.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

class AdminNotificationControllerTest {

    @Test
    void shouldRegisterAdminMessageRoutesWithoutDuplicateMappings() {
        AdminNotificationController controller = new AdminNotificationController(mock(NotificationService.class));

        assertDoesNotThrow(() -> MockMvcBuilders.standaloneSetup(controller).build());
    }
}
