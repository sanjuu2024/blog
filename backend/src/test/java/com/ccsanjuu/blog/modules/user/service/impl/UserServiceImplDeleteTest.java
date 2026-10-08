package com.ccsanjuu.blog.modules.user.service.impl;

import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.auth.service.AuthService;
import com.ccsanjuu.blog.modules.auth.service.EmailVerificationService;
import com.ccsanjuu.blog.modules.auth.service.TokenVersionService;
import com.ccsanjuu.blog.modules.file.service.ImageUploadService;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.dto.AdminDeleteUserRequestDTO;
import com.ccsanjuu.blog.modules.user.model.dto.DeleteCurrentUserRequestDTO;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import com.ccsanjuu.blog.modules.user.model.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplDeleteTest {

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserMapper userMapper;
    @Mock
    private AuthService authService;
    @Mock
    private TokenVersionService tokenVersionService;
    @Mock
    private ImageUploadService imageUploadService;
    @Mock
    private EmailVerificationService emailVerificationService;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(
                passwordEncoder, userMapper, authService, tokenVersionService,
                imageUploadService, emailVerificationService,
                org.mockito.Mockito.mock(com.ccsanjuu.blog.modules.security.service.SecurityEventService.class));
    }

    @Test
    void currentUserDeleteShouldAnonymizeAndInvalidateSessions() {
        User user = User.builder()
                .id(10001L)
                .username("alice")
                .email("alice@example.com")
                .passwordHash("hash")
                .build();
        when(userMapper.selectByIdForUpdate(10001L)).thenReturn(user);
        when(passwordEncoder.matches("Password_1", "hash")).thenReturn(true);

        userService.deleteCurrentUser(10001L, new DeleteCurrentUserRequestDTO("Password_1"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertEquals("deleted_10001", captor.getValue().getUsername());
        assertEquals("deleted_10001@deleted.invalid", captor.getValue().getEmail());
        assertEquals("账号已注销", captor.getValue().getNickname());
        verify(tokenVersionService).incrementVersion(10001L);
        verify(authService).revokeUserRefreshTokens(10001L);
    }

    @Test
    void adminDeleteShouldRejectLastAvailableAdmin() {
        when(userMapper.selectByIdForUpdate(10002L)).thenReturn(User.builder()
                .id(10002L).role(UserRole.ADMIN).build());
        when(userMapper.selectActiveAdminIdsForUpdate()).thenReturn(List.of(10002L));

        BizException exception = assertThrows(BizException.class, () -> userService.deleteUserByAdmin(
                10001L, 10002L, new AdminDeleteUserRequestDTO("账号注销申请")));

        assertEquals(ResultCode.LAST_ADMIN_DELETE_NOT_ALLOWED, exception.getResultCode());
    }

    @Test
    void adminShouldNotBeAbleToDeleteOwnAccount() {
        when(userMapper.selectByIdForUpdate(10001L)).thenReturn(User.builder()
                .id(10001L).role(UserRole.ADMIN).passwordHash("hash").build());
        when(passwordEncoder.matches("Password_1", "hash")).thenReturn(true);
        BizException exception = assertThrows(BizException.class, () -> userService.deleteCurrentUser(
                10001L, new DeleteCurrentUserRequestDTO("Password_1")));

        assertEquals(ResultCode.SELF_USER_DELETE_NOT_ALLOWED, exception.getResultCode());
    }
}
