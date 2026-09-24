package com.ccsanjuu.blog.modules.user.service.impl;

import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.dto.AdminBootstrapRequestDTO;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import com.ccsanjuu.blog.modules.user.model.enums.UserRole;
import com.ccsanjuu.blog.modules.user.model.enums.UserStatus;
import jakarta.validation.Validation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductionAdminBootstrapServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    private ProductionAdminBootstrapServiceImpl bootstrapService;

    @BeforeEach
    void setUp() {
        bootstrapService = new ProductionAdminBootstrapServiceImpl(
                userMapper,
                passwordEncoder,
                Validation.buildDefaultValidatorFactory().getValidator()
        );
    }

    @Test
    void shouldCreateActiveAdministratorWhenDatabaseHasNoAdministratorOrConflict() {
        when(userMapper.selectCount(any())).thenReturn(0L, 0L, 0L);
        when(passwordEncoder.encode("StrongPass1!")).thenReturn("encoded-password");
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(42L);
            return 1;
        });

        Long userId = bootstrapService.bootstrap(validRequest());

        assertEquals(42L, userId);
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(userCaptor.capture());
        User admin = userCaptor.getValue();
        assertEquals("prod_admin", admin.getUsername());
        assertEquals("站点管理员", admin.getNickname());
        assertEquals("admin@example.com", admin.getEmail());
        assertEquals("encoded-password", admin.getPasswordHash());
        assertEquals(UserRole.ADMIN, admin.getRole());
        assertEquals(UserStatus.ACTIVE, admin.getStatus());
        assertEquals(0L, admin.getTokenVersion());
        assertEquals("", admin.getAvatarUrl());
        assertEquals("", admin.getBio());
        assertEquals(false, admin.getEmailVerified());
        verify(userMapper, times(3)).selectCount(any());
    }

    @Test
    void shouldRejectBootstrapWhenAdministratorAlreadyExists() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> bootstrapService.bootstrap(validRequest())
        );

        assertTrue(exception.getMessage().contains("已存在管理员"));
        verify(userMapper, never()).insert(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void shouldRejectBootstrapWhenUsernameAlreadyExists() {
        when(userMapper.selectCount(any())).thenReturn(0L, 1L);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> bootstrapService.bootstrap(validRequest())
        );

        assertTrue(exception.getMessage().contains("用户名已存在"));
        verify(userMapper, never()).insert(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void shouldRejectBootstrapWhenEmailAlreadyExists() {
        when(userMapper.selectCount(any())).thenReturn(0L, 0L, 1L);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> bootstrapService.bootstrap(validRequest())
        );

        assertTrue(exception.getMessage().contains("邮箱已存在"));
        verify(userMapper, never()).insert(any(User.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void shouldFailWhenAdministratorRecordIsNotInserted() {
        when(userMapper.selectCount(any())).thenReturn(0L, 0L, 0L);
        when(passwordEncoder.encode("StrongPass1!")).thenReturn("encoded-password");
        when(userMapper.insert(any(User.class))).thenReturn(0);

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> bootstrapService.bootstrap(validRequest())
        );

        assertTrue(exception.getMessage().contains("未成功写入"));
    }

    @Test
    void shouldRejectInvalidPasswordBeforeQueryingDatabase() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> bootstrapService.bootstrap(new AdminBootstrapRequestDTO(
                        "prod_admin",
                        "站点管理员",
                        "admin@example.com",
                        "bad password"
                ))
        );

        assertTrue(exception.getMessage().contains("password"));
        verifyNoInteractions(userMapper, passwordEncoder);
    }

    @Test
    void shouldRejectMissingRequestBeforeQueryingDatabase() {
        assertThrows(IllegalArgumentException.class, () -> bootstrapService.bootstrap(null));

        verifyNoInteractions(userMapper, passwordEncoder);
    }

    private AdminBootstrapRequestDTO validRequest() {
        return new AdminBootstrapRequestDTO(
                "prod_admin",
                "站点管理员",
                "admin@example.com",
                "StrongPass1!"
        );
    }
}
