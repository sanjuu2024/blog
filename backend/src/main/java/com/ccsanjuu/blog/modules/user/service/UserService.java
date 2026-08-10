package com.ccsanjuu.blog.modules.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.modules.user.model.dto.*;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import com.ccsanjuu.blog.modules.user.model.vo.*;
import org.springframework.web.multipart.MultipartFile;

public interface UserService extends IService<User> {
    /**
     * 获取用户公开资料卡
     * @param userId
     * @return
     */
    PublicUserProfileVO getPublicUserProfile(Long userId);

    /**
     * 获取当前登录用户信息
     * @param userId
     * @return
     */
    CurrentUserProfileVO getCurrentUserProfile(Long userId);

    /**
     * 更新个人资料
     * @param userId
     * @param updateProfileRequestDTO
     * @return
     */
    UpdatedUserProfileVO updateProfile(Long userId, UpdateProfileRequestDTO updateProfileRequestDTO);

    /**
     * 上传并更新当前用户头像。
     *
     * @param userId 用户 ID
     * @param file 头像图片
     * @return 更新后的头像信息
     */
    UpdatedUserAvatarVO updateAvatar(Long userId, MultipartFile file);

    /**
     * 修改密码
     * @param userId
     * @param changePasswordRequestDTO
     * @return
     */
    Void changePassword(Long userId, ChangePasswordRequestDTO changePasswordRequestDTO);

    /**
     * 获取用户分页列表
     * @param userManagementPageQueryDTO
     * @return
     */
    PageResult<AdminUserItemVO> userPageQuery(UserManagementPageQueryDTO userManagementPageQueryDTO);

    /**
     * 修改用户状态
     * @param currentUserId 当前登录的用户 id
     * @param userId 要修改角色的目标用户 id
     * @param updateUserStatusRequestDTO
     * @return
     */
    UpdatedUserStatusVO changeUserStatus(Long currentUserId, Long userId, UpdateUserStatusRequestDTO updateUserStatusRequestDTO);

    /**
     * 修改用户角色
     * @param currentUserId 当前登录的用户 id
     * @param userId 要修改角色的目标用户 id
     * @param updateUserRoleRequestDTO
     * @return
     */
    UpdatedUserRoleVO changeUserRole(Long currentUserId, Long userId, UpdateUserRoleRequestDTO updateUserRoleRequestDTO);
}
