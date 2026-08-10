package com.ccsanjuu.blog.modules.file;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.file.service.ObjectStorageService;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import com.ccsanjuu.blog.modules.user.model.enums.UserStatus;
import com.ccsanjuu.blog.modules.user.model.vo.UpdatedUserAvatarVO;
import com.ccsanjuu.blog.modules.user.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class ImageUploadIntegrationTest {

    private static final String USERNAME = "demo_alice";
    private static final String AVATAR_URL = "https://img.test.example.com/avatars/integration-test.png";

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @MockitoBean
    private ObjectStorageService objectStorageService;

    private String minuteRateLimitKey;
    private String dailyRateLimitKey;

    @AfterEach
    void clearAvatarRateLimitKeys() {
        if (minuteRateLimitKey != null) {
            stringRedisTemplate.delete(minuteRateLimitKey);
        }
        if (dailyRateLimitKey != null) {
            stringRedisTemplate.delete(dailyRateLimitKey);
        }
    }

    @Test
    void avatarUploadShouldPersistUrlAndUseRealRedisRateLimit() {
        User user = findActiveUser();
        minuteRateLimitKey = buildRateLimitKey(user.getId(), "minute");
        dailyRateLimitKey = buildRateLimitKey(user.getId(), "day");
        stringRedisTemplate.delete(minuteRateLimitKey);
        stringRedisTemplate.delete(dailyRateLimitKey);
        when(objectStorageService.upload(
                anyString(),
                any(InputStream.class),
                anyLong(),
                eq("image/png")
        )).thenReturn(AVATAR_URL);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}
        );

        UpdatedUserAvatarVO result = userService.updateAvatar(user.getId(), file);
        User updatedUser = userMapper.selectById(user.getId());

        assertEquals(AVATAR_URL, result.getAvatarUrl());
        assertEquals(AVATAR_URL, updatedUser.getAvatarUrl());
        assertTrue(Boolean.TRUE.equals(stringRedisTemplate.hasKey(minuteRateLimitKey)));
        assertTrue(Boolean.TRUE.equals(stringRedisTemplate.hasKey(dailyRateLimitKey)));

        BizException exception = assertThrows(
                BizException.class,
                () -> userService.updateAvatar(user.getId(), file)
        );
        assertEquals(ResultCode.AVATAR_UPLOAD_RATE_LIMITED, exception.getResultCode());
        verify(objectStorageService, times(1))
                .upload(anyString(), any(InputStream.class), anyLong(), eq("image/png"));
    }

    private User findActiveUser() {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, USERNAME)
                .eq(User::getStatus, UserStatus.ACTIVE));
        assertNotNull(user);
        return user;
    }

    private String buildRateLimitKey(Long userId, String period) {
        return "blog:image:avatar:rate:user:" + userId + ":" + period;
    }
}
