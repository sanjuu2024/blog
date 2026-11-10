package com.ccsanjuu.blog.modules.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccsanjuu.blog.modules.auth.service.TokenVersionService;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import com.ccsanjuu.blog.modules.user.model.enums.UserStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest
class TokenVersionIntegrationTest {

    private static final String USERNAME = "demo_alice";

    @Autowired
    private TokenVersionService tokenVersionService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private String tokenVersionKey;

    @AfterEach
    void clearTokenVersionCache() {
        if (tokenVersionKey != null) {
            stringRedisTemplate.delete(tokenVersionKey);
        }
    }

    @Test
    void cacheMissShouldLoadTokenVersionFromDatabase() {
        User user = findActiveUser();
        tokenVersionKey = buildKey(user.getId());
        stringRedisTemplate.delete(tokenVersionKey);

        Long version = tokenVersionService.getCurrentVersion(user.getId());

        assertEquals(user.getTokenVersion(), version);
        assertEquals(String.valueOf(version), stringRedisTemplate.opsForValue().get(tokenVersionKey));
        assertTrue(stringRedisTemplate.getExpire(tokenVersionKey) > 0);
    }

    @Test
    void transactionRollbackShouldRestoreDatabaseAndRedisVersion() {
        User user = findActiveUser();
        Long originalVersion = user.getTokenVersion();
        tokenVersionKey = buildKey(user.getId());
        stringRedisTemplate.opsForValue().set(tokenVersionKey, String.valueOf(originalVersion));
        AtomicReference<Long> incrementedVersion = new AtomicReference<>();

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(status -> {
            incrementedVersion.set(tokenVersionService.incrementVersion(user.getId()));
            String cachedValue = stringRedisTemplate.opsForValue().get(tokenVersionKey);
            assertNotNull(cachedValue);
            assertTrue(cachedValue.startsWith(incrementedVersion.get() + ":"));
            status.setRollbackOnly();
        });

        assertEquals(originalVersion + 1, incrementedVersion.get());
        assertEquals(originalVersion, userMapper.selectById(user.getId()).getTokenVersion());
        assertEquals(String.valueOf(originalVersion), stringRedisTemplate.opsForValue().get(tokenVersionKey));
    }

    @Test
    void transactionRollbackShouldNotOverwriteAnotherTransactionCacheValue() {
        User user = findActiveUser();
        Long originalVersion = user.getTokenVersion();
        tokenVersionKey = buildKey(user.getId());
        String anotherTransactionValue = (originalVersion + 1) + ":another-operation";

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.executeWithoutResult(status -> {
            tokenVersionService.incrementVersion(user.getId());
            stringRedisTemplate.opsForValue().set(tokenVersionKey, anotherTransactionValue);
            status.setRollbackOnly();
        });

        assertEquals(originalVersion, userMapper.selectById(user.getId()).getTokenVersion());
        assertEquals(anotherTransactionValue, stringRedisTemplate.opsForValue().get(tokenVersionKey));
    }

    private User findActiveUser() {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>()
                        .eq(User::getUsername, USERNAME)
                        .eq(User::getStatus, UserStatus.ACTIVE)
        );
        assertNotNull(user);
        return user;
    }

    private String buildKey(Long userId) {
        return "blog:auth:token-version:user:" + userId;
    }
}
