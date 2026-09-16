package com.ccsanjuu.blog.modules.user.service.impl;

import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.auth.service.AuthService;
import com.ccsanjuu.blog.modules.auth.service.TokenVersionService;
import com.ccsanjuu.blog.modules.file.service.ImageUploadService;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.dto.UpdateProfileRequestDTO;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import com.ccsanjuu.blog.modules.user.model.enums.UserRole;
import com.ccsanjuu.blog.modules.user.model.enums.UserStatus;
import com.ccsanjuu.blog.modules.user.model.vo.CurrentUserProfileVO;
import com.ccsanjuu.blog.modules.user.model.vo.PublicUserProfileVO;
import com.ccsanjuu.blog.modules.user.model.vo.UpdatedUserProfileVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplProfileTest {

    private static final Long USER_ID = 10001L;

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

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(
                passwordEncoder,
                userMapper,
                authService,
                tokenVersionService,
                imageUploadService
        );
    }

    @Test
    void getPublicUserProfileShouldOnlyMapPublicFields() {
        when(userMapper.selectById(USER_ID)).thenReturn(user());

        PublicUserProfileVO result = userService.getPublicUserProfile(USER_ID);

        assertEquals(USER_ID, result.getId());
        assertEquals("demo_user", result.getUsername());
        assertEquals("旧昵称", result.getNickname());
        assertEquals("https://img.example.com/avatar.jpg", result.getAvatarUrl());
        assertEquals("旧简介", result.getBio());
    }

    @Test
    void getCurrentUserProfileShouldMapAccountAndStatusFields() {
        when(userMapper.selectById(USER_ID)).thenReturn(user());

        CurrentUserProfileVO result = userService.getCurrentUserProfile(USER_ID);

        assertEquals(USER_ID, result.getId());
        assertEquals("demo@example.com", result.getEmail());
        assertEquals(UserRole.USER, result.getRole());
        assertEquals(UserStatus.ACTIVE, result.getStatus());
    }

    @Test
    void updateProfileShouldOnlyPersistProvidedFieldsAndReturnMergedProfile() {
        OffsetDateTime updatedAt = OffsetDateTime.of(2026, 9, 15, 12, 0, 0, 0, ZoneOffset.UTC);
        when(userMapper.selectById(USER_ID)).thenReturn(user());
        doAnswer(invocation -> {
            User update = invocation.getArgument(0);
            update.setUpdatedAt(updatedAt);
            return 1;
        }).when(userMapper).updateById(any(User.class));
        UpdateProfileRequestDTO requestDTO = UpdateProfileRequestDTO.builder()
                .nickname("新昵称")
                .build();

        UpdatedUserProfileVO result = userService.updateProfile(USER_ID, requestDTO);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertEquals(USER_ID, captor.getValue().getId());
        assertEquals("新昵称", captor.getValue().getNickname());
        assertNull(captor.getValue().getBio());
        assertEquals("新昵称", result.getNickname());
        assertEquals("旧简介", result.getBio());
        assertEquals(updatedAt, result.getUpdatedAt());
    }

    @Test
    void updateProfileShouldSkipDatabaseUpdateWhenNoFieldIsProvided() {
        when(userMapper.selectById(USER_ID)).thenReturn(user());

        UpdatedUserProfileVO result = userService.updateProfile(
                USER_ID,
                new UpdateProfileRequestDTO()
        );

        verify(userMapper, never()).updateById(any(User.class));
        assertEquals("旧昵称", result.getNickname());
        assertEquals("旧简介", result.getBio());
        assertNull(result.getUpdatedAt());
        verifyNoInteractions(authService, tokenVersionService, imageUploadService);
    }

    @Test
    void getProfileShouldRejectMissingUser() {
        when(userMapper.selectById(USER_ID)).thenReturn(null);

        BizException exception = assertThrows(
                BizException.class,
                () -> userService.getCurrentUserProfile(USER_ID)
        );

        assertEquals(ResultCode.USER_NOT_FOUND, exception.getResultCode());
    }

    private User user() {
        return User.builder()
                .id(USER_ID)
                .username("demo_user")
                .nickname("旧昵称")
                .email("demo@example.com")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .avatarUrl("https://img.example.com/avatar.jpg")
                .bio("旧简介")
                .build();
    }
}
