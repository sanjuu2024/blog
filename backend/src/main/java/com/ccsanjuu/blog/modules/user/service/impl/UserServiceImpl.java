package com.ccsanjuu.blog.modules.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.auth.service.AuthService;
import com.ccsanjuu.blog.modules.auth.service.TokenVersionService;
import com.ccsanjuu.blog.modules.file.model.vo.UploadedImageVO;
import com.ccsanjuu.blog.modules.file.service.ImageUploadService;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.dto.*;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import com.ccsanjuu.blog.modules.user.model.enums.UserRole;
import com.ccsanjuu.blog.modules.user.model.enums.UserStatus;
import com.ccsanjuu.blog.modules.user.model.vo.*;
import com.ccsanjuu.blog.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuthService authService;
    private final TokenVersionService tokenVersionService;
    private final ImageUploadService imageUploadService;

    /**
     * 获取用户公开资料卡
     * @param userId
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public PublicUserProfileVO getPublicUserProfile(Long userId) {
        User user = requireUser(userId);
        return BeanUtil.copyProperties(user, PublicUserProfileVO.class);
    }

    /**
     * 获取当前登录用户信息
     * @param userId
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public CurrentUserProfileVO getCurrentUserProfile(Long userId) {
        User user = requireUser(userId);
        return BeanUtil.copyProperties(user, CurrentUserProfileVO.class);
    }

    /**
     * 更新个人资料
     * @param userId
     * @param updateProfileRequestDTO
     * @return
     */
    @Override
    public UpdatedUserProfileVO updateProfile(Long userId, UpdateProfileRequestDTO updateProfileRequestDTO) {
        User user = requireUser(userId);

        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setNickname(updateProfileRequestDTO.getNickname());
        updateUser.setBio(updateProfileRequestDTO.getBio());
        if (updateUser.getNickname() != null || updateUser.getBio() != null) {
            userMapper.updateById(updateUser);
        }

        UpdatedUserProfileVO updatedUserProfileVO = BeanUtil.copyProperties(user, UpdatedUserProfileVO.class);
        updatedUserProfileVO.setNickname(
                updateProfileRequestDTO.getNickname() != null
                ? updateProfileRequestDTO.getNickname()
                : user.getNickname()
        );
        updatedUserProfileVO.setBio(
                updateProfileRequestDTO.getBio() != null
                ? updateProfileRequestDTO.getBio()
                : user.getBio()
        );
        if (updateUser.getUpdatedAt() != null) {
            updatedUserProfileVO.setUpdatedAt(updateUser.getUpdatedAt());
        }
        return updatedUserProfileVO;
    }

    /**
     * 上传并更新当前用户头像。
     *
     * @param userId 用户 ID
     * @param file 头像图片
     * @return 更新后的头像信息
     */
    @Override
    @Transactional
    public UpdatedUserAvatarVO updateAvatar(Long userId, MultipartFile file) {
        User user = requireUser(userId);
        UploadedImageVO uploadedImage = imageUploadService.uploadAvatar(userId, file);

        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setAvatarUrl(uploadedImage.getUrl());
        userMapper.updateById(updateUser);

        return UpdatedUserAvatarVO.builder()
                .id(user.getId())
                .avatarUrl(uploadedImage.getUrl())
                .updatedAt(updateUser.getUpdatedAt())
                .build();
    }

    /**
     * 修改密码
     * @param userId
     * @param changePasswordRequestDTO
     * @return
     */
    @Override
    @Transactional
    public Void changePassword(Long userId, ChangePasswordRequestDTO changePasswordRequestDTO) {
        User user = requireUserForUpdate(userId);
        if (!passwordEncoder.matches(changePasswordRequestDTO.getOldPassword(), user.getPasswordHash())){
            log.warn(
                    "security_event=PASSWORD_CHANGE_FAILED description=\"修改密码失败：原密码错误\" outcome=FAIL reason=OLD_PASSWORD_ERROR userId={}",
                    userId
            );
            throw new BizException(ResultCode.OLD_PASSWORD_ERROR);
        }

        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setPasswordHash(passwordEncoder.encode(changePasswordRequestDTO.getNewPassword()));
        userMapper.updateById(updateUser);
        tokenVersionService.incrementVersion(user.getId());
        authService.revokeUserRefreshTokens(user.getId());   // 用户修改密码，则撤销其现有的活跃 RT

        log.info(
                "security_event=PASSWORD_CHANGE_SUCCESS description=\"修改密码成功，已撤销现有登录态\" outcome=SUCCESS userId={} refreshTokensRevoked=true",
                userId
        );

        return null;
    }

    /**
     * 获取用户分页列表
     * @param userManagementPageQueryDTO
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminUserItemVO> userPageQuery(UserManagementPageQueryDTO userManagementPageQueryDTO) {
        // 1. 构建分页查询条件，并执行分页查询
        Page<User> page = Page.of(userManagementPageQueryDTO.getPageNum(), userManagementPageQueryDTO.getPageSize());
        String username = StringUtils.hasText(userManagementPageQueryDTO.getUsername())
                ? userManagementPageQueryDTO.getUsername().trim()
                : null;
        String email = StringUtils.hasText(userManagementPageQueryDTO.getEmail())
                ? userManagementPageQueryDTO.getEmail().trim()
                : null;

        lambdaQuery()
                .select(User::getId,User::getUsername,User::getNickname,User::getEmail,User::getRole,User::getStatus,User::getLastLoginAt,User::getCreatedAt)
                .eq(userManagementPageQueryDTO.getRole() != null, User::getRole, userManagementPageQueryDTO.getRole())
                .eq(userManagementPageQueryDTO.getStatus() != null, User::getStatus, userManagementPageQueryDTO.getStatus())
                .apply(
                        StringUtils.hasText(username),
                        "STRPOS(LOWER(username), LOWER({0})) > 0",
                        username
                )
                .apply(
                        StringUtils.hasText(email),
                        "STRPOS(LOWER(email), LOWER({0})) > 0",
                        email
                )
                .orderByDesc(User::getCreatedAt)
                .orderByDesc(User::getId)
                .page(page);

        List<AdminUserItemVO> records = BeanUtil.copyToList(page.getRecords(), AdminUserItemVO.class);
        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    /**
     * 修改用户状态
     * @param currentUserId 当前登录的用户 id
     * @param userId 要修改角色的目标用户 id
     * @param updateUserStatusRequestDTO
     * @return
     */
    @Override
    @Transactional
    public UpdatedUserStatusVO changeUserStatus(Long currentUserId, Long userId, UpdateUserStatusRequestDTO updateUserStatusRequestDTO) {
        User user = requireUserForUpdate(userId);
        if (currentUserId.equals(user.getId())) {   // 管理员不得修改自己的状态
            log.warn(
                    "security_event=USER_STATUS_CHANGE_FAILED description=\"修改用户状态失败：不能修改自己的状态\" outcome=FAIL reason=SELF_CHANGE actorId={} targetUserId={}",
                    currentUserId,
                    userId
            );
            throw new BizException(ResultCode.SELF_STATUS_CHANGE_NOT_ALLOWED);
        }
        if (user.getStatus() != updateUserStatusRequestDTO.getStatus()) {
            UserStatus oldStatus = user.getStatus();
            User updateUser = new User();
            updateUser.setId(user.getId());
            updateUser.setStatus(updateUserStatusRequestDTO.getStatus());
            userMapper.updateById(updateUser);
            if (updateUserStatusRequestDTO.getStatus() == UserStatus.DISABLED) {
                tokenVersionService.incrementVersion(user.getId());
                authService.revokeUserRefreshTokens(user.getId());   // 用户状态从 ACTIVE 变成 DISABLED，则撤销其现有的活跃 RT
            }
            log.info(
                    "security_event=USER_STATUS_CHANGED description=\"用户状态修改成功\" outcome=SUCCESS actorId={} targetUserId={} oldStatus={} newStatus={} refreshTokensRevoked={}",
                    currentUserId,
                    userId,
                    oldStatus,
                    updateUserStatusRequestDTO.getStatus(),
                    updateUserStatusRequestDTO.getStatus() == UserStatus.DISABLED
            );
        }
        return UpdatedUserStatusVO.builder()
                .id(user.getId())
                .status(updateUserStatusRequestDTO.getStatus())
                .build();
    }

    /**
     * 修改用户角色
     * @param currentUserId 当前登录的用户 id
     * @param userId 要修改角色的目标用户 id
     * @param updateUserRoleRequestDTO
     * @return
     */
    @Override
    @Transactional
    public UpdatedUserRoleVO changeUserRole(Long currentUserId, Long userId, UpdateUserRoleRequestDTO updateUserRoleRequestDTO) {
        User user = requireUserForUpdate(userId);
        if (currentUserId.equals(user.getId())) {   // 管理员不得修改自己的角色
            log.warn(
                    "security_event=USER_ROLE_CHANGE_FAILED description=\"修改用户角色失败：不能修改自己的角色\" outcome=FAIL reason=SELF_CHANGE actorId={} targetUserId={}",
                    currentUserId,
                    userId
            );
            throw new BizException(ResultCode.SELF_ROLE_CHANGE_NOT_ALLOWED);
        }
        if (user.getRole() != updateUserRoleRequestDTO.getRole()) {
            UserRole oldRole = user.getRole();
            User updateUser = new User();
            updateUser.setId(user.getId());
            updateUser.setRole(updateUserRoleRequestDTO.getRole());
            userMapper.updateById(updateUser);
            tokenVersionService.incrementVersion(user.getId());
            log.info(
                    "security_event=USER_ROLE_CHANGED description=\"用户角色修改成功\" outcome=SUCCESS actorId={} targetUserId={} oldRole={} newRole={}",
                    currentUserId,
                    userId,
                    oldRole,
                    updateUserRoleRequestDTO.getRole()
            );
        }
        return UpdatedUserRoleVO.builder()
                .id(user.getId())
                .role(updateUserRoleRequestDTO.getRole())
                .build();
    }

    /**
     * 根据 userId 查询并锁定用户数据，用户不存在则抛出业务异常
     * @param userId
     * @return
     */
    private User requireUserForUpdate(Long userId) {
        User user = userMapper.selectByIdForUpdate(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        return user;
    }

    /**
     * 根据 userId 获取用户数据，用户不存在则抛出业务异常
     * @param userId
     * @return
     */
    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        return user;
    }
}
