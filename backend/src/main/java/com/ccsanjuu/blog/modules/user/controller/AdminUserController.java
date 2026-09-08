package com.ccsanjuu.blog.modules.user.controller;

import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.modules.audit.annotation.AdminAudit;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditAction;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditResourceType;
import com.ccsanjuu.blog.modules.auth.model.security.JwtPrincipal;
import com.ccsanjuu.blog.modules.user.model.dto.UpdateUserRoleRequestDTO;
import com.ccsanjuu.blog.modules.user.model.dto.UpdateUserStatusRequestDTO;
import com.ccsanjuu.blog.modules.user.model.dto.UserManagementPageQueryDTO;
import com.ccsanjuu.blog.modules.user.model.vo.AdminUserItemVO;
import com.ccsanjuu.blog.modules.user.model.vo.UpdatedUserRoleVO;
import com.ccsanjuu.blog.modules.user.model.vo.UpdatedUserStatusVO;
import com.ccsanjuu.blog.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Tag(name = "用户管理接口")
public class AdminUserController {

    private final UserService userService;

    /**
     * 获取用户分页列表
     * @param userManagementPageQueryDTO
     * @return
     */
    @GetMapping
    @Operation(description = "获取用户分页列表")
    public Result<PageResult<AdminUserItemVO>> userPageQuery(@Valid @ModelAttribute UserManagementPageQueryDTO userManagementPageQueryDTO) {
        return Result.success(userService.userPageQuery(userManagementPageQueryDTO));
    }

    /**
     * 修改用户状态
     * @param userId 要修改角色的目标用户 id
     * @param updateUserStatusRequestDTO
     * @param jwtPrincipal
     * @return
     */
    @PatchMapping("/{userId}/status")
    @Operation(description = "修改用户状态")
    @AdminAudit(
            resourceType = AdminAuditResourceType.USER,
            action = AdminAuditAction.CHANGE_STATUS,
            resourceId = "#p0",
            detail = "#p1.status"
    )
    public Result<UpdatedUserStatusVO> changeUserStatus(
            @PathVariable @Positive Long userId,
            @Valid @RequestBody UpdateUserStatusRequestDTO updateUserStatusRequestDTO,
            @AuthenticationPrincipal JwtPrincipal jwtPrincipal
    ) {
        return Result.success(userService.changeUserStatus(jwtPrincipal.userId(), userId, updateUserStatusRequestDTO));
    }

    /**
     * 修改用户角色
     * @param userId 要修改角色的目标用户 id
     * @param updateUserRoleRequestDTO
     * @param jwtPrincipal
     * @return
     */
    @PatchMapping("/{userId}/role")
    @Operation(description = "修改用户角色")
    @AdminAudit(
            resourceType = AdminAuditResourceType.USER,
            action = AdminAuditAction.CHANGE_ROLE,
            resourceId = "#p0",
            detail = "#p1.role"
    )
    public Result<UpdatedUserRoleVO> changeUserRole(
            @PathVariable @Positive Long userId,
            @Valid @RequestBody UpdateUserRoleRequestDTO updateUserRoleRequestDTO,
            @AuthenticationPrincipal JwtPrincipal jwtPrincipal
            ) {
        return Result.success(userService.changeUserRole(jwtPrincipal.userId(), userId, updateUserRoleRequestDTO));
    }
}
