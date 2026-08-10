package com.ccsanjuu.blog.modules.user.service.impl;

import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.auth.service.AuthService;
import com.ccsanjuu.blog.modules.auth.service.TokenVersionService;
import com.ccsanjuu.blog.modules.file.model.vo.UploadedImageVO;
import com.ccsanjuu.blog.modules.file.service.ImageUploadService;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import com.ccsanjuu.blog.modules.user.model.vo.UpdatedUserAvatarVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplAvatarTest {

    private static final Long USER_ID = 10001L;
    private static final String AVATAR_URL = "https://img.example.com/avatars/10001/avatar.jpg";

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
    void updateAvatarShouldUploadImageAndPersistPublicUrl() {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", new byte[]{1});
        when(userMapper.selectById(USER_ID)).thenReturn(User.builder().id(USER_ID).build());
        when(imageUploadService.uploadAvatar(USER_ID, file)).thenReturn(UploadedImageVO.builder()
                .url(AVATAR_URL)
                .build());

        UpdatedUserAvatarVO result = userService.updateAvatar(USER_ID, file);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(userCaptor.capture());
        assertEquals(USER_ID, userCaptor.getValue().getId());
        assertEquals(AVATAR_URL, userCaptor.getValue().getAvatarUrl());
        assertEquals(USER_ID, result.getId());
        assertEquals(AVATAR_URL, result.getAvatarUrl());
    }

    @Test
    void updateAvatarShouldRejectMissingUserBeforeUploading() {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", new byte[]{1});
        when(userMapper.selectById(USER_ID)).thenReturn(null);

        BizException exception = assertThrows(
                BizException.class,
                () -> userService.updateAvatar(USER_ID, file)
        );

        assertEquals(ResultCode.USER_NOT_FOUND, exception.getResultCode());
        verifyNoInteractions(imageUploadService);
    }
}
