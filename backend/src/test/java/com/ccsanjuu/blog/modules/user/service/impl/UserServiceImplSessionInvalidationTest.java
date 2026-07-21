package com.ccsanjuu.blog.modules.user.service.impl;

import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.auth.service.AuthService;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.dto.ChangePasswordRequestDTO;
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
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
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
    void changePasswordShouldLogSuccessWithoutExposingPassword(CapturedOutput output) {
        String newPassword = "NewPassword_123";
        when(userMapper.selectById(USER_ID)).thenReturn(User.builder()
                .id(USER_ID)
                .passwordHash("old-password-hash")
                .build());
        when(passwordEncoder.matches("OldPassword_123", "old-password-hash")).thenReturn(true);
        when(passwordEncoder.encode(newPassword)).thenReturn("new-password-hash");
        ChangePasswordRequestDTO request = new ChangePasswordRequestDTO();
        request.setOldPassword("OldPassword_123");
        request.setNewPassword(newPassword);

        userService.changePassword(USER_ID, request);

        verify(userMapper).updateById(any(User.class));
        verify(authService).revokeUserRefreshTokens(USER_ID);
        assertTrue(output.getOut().contains("security_event=PASSWORD_CHANGE_SUCCESS"));
        assertTrue(output.getOut().contains("description=\"修改密码成功，已撤销现有登录态\""));
        assertFalse(output.getOut().contains(newPassword));
        assertFalse(output.getOut().contains("new-password-hash"));
    }

    @Test
    void changePasswordShouldLogOldPasswordFailureWithoutExposingPassword(CapturedOutput output) {
        String oldPassword = "WrongPassword_123";
        when(userMapper.selectById(USER_ID)).thenReturn(User.builder()
                .id(USER_ID)
                .passwordHash("old-password-hash")
                .build());
        when(passwordEncoder.matches(oldPassword, "old-password-hash")).thenReturn(false);
        ChangePasswordRequestDTO request = new ChangePasswordRequestDTO();
        request.setOldPassword(oldPassword);
        request.setNewPassword("NewPassword_123");

        BizException exception = assertThrows(
                BizException.class,
                () -> userService.changePassword(USER_ID, request)
        );

        assertEquals(ResultCode.OLD_PASSWORD_ERROR, exception.getResultCode());
        assertTrue(output.getOut().contains("security_event=PASSWORD_CHANGE_FAILED"));
        assertFalse(output.getOut().contains(oldPassword));
        verify(userMapper, never()).updateById(any(User.class));
        verifyNoInteractions(authService);
    }

    @Test
    void changeUserStatusShouldRevokeRefreshTokensWhenStatusChanged(CapturedOutput output) {
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
        assertTrue(output.getOut().contains("security_event=USER_STATUS_CHANGED"));
        assertTrue(output.getOut().contains("actorId=" + CURRENT_USER_ID));
        assertTrue(output.getOut().contains("targetUserId=" + USER_ID));
        assertTrue(output.getOut().contains("oldStatus=ACTIVE newStatus=DISABLED"));
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
    void changeUserStatusShouldRejectSelfChange(CapturedOutput output) {
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
        assertTrue(output.getOut().contains("security_event=USER_STATUS_CHANGE_FAILED"));
        assertTrue(output.getOut().contains("reason=SELF_CHANGE"));
    }

    @Test
    void changeUserRoleShouldUpdateRoleWithoutRevokingRefreshTokens(CapturedOutput output) {
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
        assertTrue(output.getOut().contains("security_event=USER_ROLE_CHANGED"));
        assertTrue(output.getOut().contains("oldRole=USER newRole=ADMIN"));
    }

    @Test
    void changeUserRoleShouldRejectSelfChange(CapturedOutput output) {
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
        assertTrue(output.getOut().contains("security_event=USER_ROLE_CHANGE_FAILED"));
        assertTrue(output.getOut().contains("reason=SELF_CHANGE"));
    }
}
