package com.ccsanjuu.blog.modules.user.service.impl;

import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.auth.service.AuthService;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.dto.UpdateUserRoleRequestDTO;
import com.ccsanjuu.blog.modules.user.model.dto.UpdateUserStatusRequestDTO;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import com.ccsanjuu.blog.modules.user.model.enums.UserRole;
import com.ccsanjuu.blog.modules.user.model.enums.UserStatus;
import com.ccsanjuu.blog.modules.user.model.vo.UpdatedUserRoleVO;
import com.ccsanjuu.blog.modules.user.model.vo.UpdatedUserStatusVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplSessionInvalidationTest {

    private static final Long CURRENT_USER_ID = 10001L;
    private static final Long USER_ID = 10002L;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @Mock
    private AuthService authService;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(passwordEncoder, userMapper, authService);
    }

    @Test
    void changeUserStatusShouldRevokeRefreshTokensWhenStatusChanged() {
        when(userMapper.selectById(USER_ID)).thenReturn(User.builder()
                .id(USER_ID)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
        UpdateUserStatusRequestDTO request = new UpdateUserStatusRequestDTO();
        request.setStatus(UserStatus.DISABLED);

        UpdatedUserStatusVO result = userService.changeUserStatus(CURRENT_USER_ID, USER_ID, request);

        assertEquals(USER_ID, result.getId());
        assertEquals(UserStatus.DISABLED, result.getStatus());
        verify(userMapper).updateById(any(User.class));
        verify(authService).revokeUserRefreshTokens(USER_ID);
    }

    @Test
    void changeUserStatusShouldSkipUpdateAndRevokeWhenStatusUnchanged() {
        when(userMapper.selectById(USER_ID)).thenReturn(User.builder()
                .id(USER_ID)
                .role(UserRole.USER)
                .status(UserStatus.DISABLED)
                .build());
        UpdateUserStatusRequestDTO request = new UpdateUserStatusRequestDTO();
        request.setStatus(UserStatus.DISABLED);

        UpdatedUserStatusVO result = userService.changeUserStatus(CURRENT_USER_ID, USER_ID, request);

        assertEquals(USER_ID, result.getId());
        assertEquals(UserStatus.DISABLED, result.getStatus());
        verify(userMapper, never()).updateById(any(User.class));
        verifyNoInteractions(authService);
    }

    @Test
    void changeUserStatusShouldSkipRevokeWhenEnabled() {
        when(userMapper.selectById(USER_ID)).thenReturn(User.builder()
                .id(USER_ID)
                .role(UserRole.USER)
                .status(UserStatus.DISABLED)
                .build());
        UpdateUserStatusRequestDTO request = new UpdateUserStatusRequestDTO();
        request.setStatus(UserStatus.ACTIVE);

        UpdatedUserStatusVO result = userService.changeUserStatus(CURRENT_USER_ID, USER_ID, request);

        assertEquals(USER_ID, result.getId());
        assertEquals(UserStatus.ACTIVE, result.getStatus());
        verify(userMapper).updateById(any(User.class));
        verifyNoInteractions(authService);
    }

    @Test
    void changeUserStatusShouldRejectSelfChange() {
        when(userMapper.selectById(CURRENT_USER_ID)).thenReturn(User.builder()
                .id(CURRENT_USER_ID)
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build());
        UpdateUserStatusRequestDTO request = new UpdateUserStatusRequestDTO();
        request.setStatus(UserStatus.DISABLED);

        BizException exception = assertThrows(BizException.class,
                () -> userService.changeUserStatus(CURRENT_USER_ID, CURRENT_USER_ID, request));

        assertEquals(ResultCode.SELF_STATUS_CHANGE_NOT_ALLOWED, exception.getResultCode());
        verify(userMapper, never()).updateById(any(User.class));
        verifyNoInteractions(authService);
    }

    @Test
    void changeUserRoleShouldUpdateRoleWithoutRevokingRefreshTokens() {
        when(userMapper.selectById(USER_ID)).thenReturn(User.builder()
                .id(USER_ID)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
        UpdateUserRoleRequestDTO request = new UpdateUserRoleRequestDTO();
        request.setRole(UserRole.ADMIN);

        UpdatedUserRoleVO result = userService.changeUserRole(CURRENT_USER_ID, USER_ID, request);

        assertEquals(USER_ID, result.getId());
        assertEquals(UserRole.ADMIN, result.getRole());
        verify(userMapper).updateById(any(User.class));
        verifyNoInteractions(authService);
    }

    @Test
    void changeUserRoleShouldRejectSelfChange() {
        when(userMapper.selectById(CURRENT_USER_ID)).thenReturn(User.builder()
                .id(CURRENT_USER_ID)
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build());
        UpdateUserRoleRequestDTO request = new UpdateUserRoleRequestDTO();
        request.setRole(UserRole.USER);

        BizException exception = assertThrows(BizException.class,
                () -> userService.changeUserRole(CURRENT_USER_ID, CURRENT_USER_ID, request));

        assertEquals(ResultCode.SELF_ROLE_CHANGE_NOT_ALLOWED, exception.getResultCode());
        verify(userMapper, never()).updateById(any(User.class));
        verifyNoInteractions(authService);
    }
}
