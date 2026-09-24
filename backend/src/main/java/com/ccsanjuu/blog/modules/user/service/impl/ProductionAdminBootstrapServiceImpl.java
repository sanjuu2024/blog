package com.ccsanjuu.blog.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.dto.AdminBootstrapRequestDTO;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import com.ccsanjuu.blog.modules.user.model.enums.UserRole;
import com.ccsanjuu.blog.modules.user.model.enums.UserStatus;
import com.ccsanjuu.blog.modules.user.service.ProductionAdminBootstrapService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductionAdminBootstrapServiceImpl implements ProductionAdminBootstrapService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final Validator validator;

    @Override
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Long bootstrap(AdminBootstrapRequestDTO request) {
        validate(request);
        if (userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getRole, UserRole.ADMIN)
        ) > 0) {
            throw new IllegalStateException("生产管理员初始化失败：系统中已存在管理员");
        }
        if (userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .apply("LOWER(username) = LOWER({0})", request.getUsername())
        ) > 0) {
            throw new IllegalStateException("生产管理员初始化失败：用户名已存在");
        }
        if (userMapper.selectCount(
                new LambdaQueryWrapper<User>()
                        .apply("LOWER(email) = LOWER({0})", request.getEmail())
        ) > 0) {
            throw new IllegalStateException("生产管理员初始化失败：邮箱已存在");
        }

        User admin = User.builder()
                .username(request.getUsername())
                .nickname(request.getNickname())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .tokenVersion(0L)
                .avatarUrl("")
                .bio("")
                .emailVerified(false)
                .build();
        if (userMapper.insert(admin) != 1 || admin.getId() == null) {
            throw new IllegalStateException("生产管理员初始化失败：管理员记录未成功写入");
        }

        log.info(
                "security_event=ADMIN_BOOTSTRAP_SUCCESS description=\"生产管理员初始化成功\" outcome=SUCCESS userId={} username={}",
                admin.getId(),
                admin.getUsername()
        );
        return admin.getId();
    }

    private void validate(AdminBootstrapRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("生产管理员初始化参数不能为空");
        }
        Set<ConstraintViolation<AdminBootstrapRequestDTO>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
                    .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                    .collect(Collectors.joining("；"));
            throw new IllegalArgumentException("生产管理员初始化参数不合法：" + message);
        }
    }
}
