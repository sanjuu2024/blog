package com.ccsanjuu.blog.modules.user.bootstrap;

import com.ccsanjuu.blog.modules.user.model.dto.AdminBootstrapRequestDTO;
import com.ccsanjuu.blog.modules.user.service.ProductionAdminBootstrapService;
import com.ccsanjuu.blog.properties.AdminBootstrapProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductionAdminBootstrapRunnerTest {

    @TempDir
    private Path tempDir;

    @Mock
    private ProductionAdminBootstrapService bootstrapService;

    @Mock
    private Environment environment;

    @Mock
    private ApplicationArguments arguments;

    @Test
    void shouldReadPasswordFileWithoutItsTrailingLineBreak() throws IOException {
        Path passwordFile = tempDir.resolve("bootstrap-password");
        Files.writeString(passwordFile, "StrongPass1!\r\n");
        AdminBootstrapProperties properties = properties(passwordFile);
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);

        new ProductionAdminBootstrapRunner(properties, bootstrapService, environment).run(arguments);

        ArgumentCaptor<AdminBootstrapRequestDTO> requestCaptor =
                ArgumentCaptor.forClass(AdminBootstrapRequestDTO.class);
        verify(bootstrapService).bootstrap(requestCaptor.capture());
        AdminBootstrapRequestDTO request = requestCaptor.getValue();
        assertEquals("prod_admin", request.getUsername());
        assertEquals("站点管理员", request.getNickname());
        assertEquals("admin@example.com", request.getEmail());
        assertEquals("StrongPass1!", request.getPassword());
    }

    @Test
    void shouldRejectBootstrapOutsideProductionProfile() {
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(false);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new ProductionAdminBootstrapRunner(
                        properties(tempDir.resolve("unused")),
                        bootstrapService,
                        environment
                ).run(arguments)
        );

        assertTrue(exception.getMessage().contains("prod profile"));
        verifyNoInteractions(bootstrapService);
    }

    @Test
    void shouldRejectMissingPasswordFile() {
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(true);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> new ProductionAdminBootstrapRunner(
                        properties(tempDir.resolve("missing")),
                        bootstrapService,
                        environment
                ).run(arguments)
        );

        assertTrue(exception.getMessage().contains("不存在或不可读"));
        verifyNoInteractions(bootstrapService);
    }

    private AdminBootstrapProperties properties(Path passwordFile) {
        AdminBootstrapProperties properties = new AdminBootstrapProperties();
        properties.setUsername(" prod_admin ");
        properties.setNickname(" 站点管理员 ");
        properties.setEmail(" admin@example.com ");
        properties.setPasswordFile(passwordFile);
        return properties;
    }
}
