package com.ccsanjuu.blog.modules.friendlink.controller;

import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.modules.audit.annotation.AdminAudit;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditAction;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditResourceType;
import com.ccsanjuu.blog.modules.friendlink.model.dto.AdminFriendLinkQueryDTO;
import com.ccsanjuu.blog.modules.friendlink.model.dto.FriendLinkUpsertRequestDTO;
import com.ccsanjuu.blog.modules.friendlink.model.dto.FriendLinkUpdateRequestDTO;
import com.ccsanjuu.blog.modules.friendlink.model.vo.AdminFriendLinkItemVO;
import com.ccsanjuu.blog.modules.friendlink.model.vo.FriendLinkMutationVO;
import com.ccsanjuu.blog.modules.friendlink.service.FriendLinkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/friend-links")
@Validated
@RequiredArgsConstructor
@Tag(name = "友链管理接口")
public class AdminFriendLinkController {

    private final FriendLinkService friendLinkService;

    /**
     * 获取后台友链分页列表。
     *
     * @param queryDTO 分页及筛选条件
     * @return 友链分页结果
     */
    @GetMapping
    @Operation(description = "获取后台友链分页列表")
    public Result<PageResult<AdminFriendLinkItemVO>> getAdminFriendLinkList(
            @Valid @ModelAttribute AdminFriendLinkQueryDTO queryDTO
    ) {
        return Result.success(friendLinkService.getAdminFriendLinkList(queryDTO));
    }

    /**
     * 创建友链。
     *
     * @param requestDTO 友链资料
     * @return 创建结果
     */
    @PostMapping
    @Operation(description = "创建友链")
    @AdminAudit(
            resourceType = AdminAuditResourceType.FRIEND_LINK,
            action = AdminAuditAction.CREATE,
            resourceId = "#result.data.id"
    )
    public Result<FriendLinkMutationVO> createFriendLink(
            @Valid @RequestBody FriendLinkUpsertRequestDTO requestDTO
    ) {
        return Result.success(friendLinkService.createFriendLink(requestDTO));
    }

    /**
     * 更新友链。
     *
     * @param friendLinkId 友链 ID
     * @param requestDTO 友链资料
     * @return 更新结果
     */
    @PutMapping("/{friendLinkId}")
    @Operation(description = "更新友链")
    @AdminAudit(
            resourceType = AdminAuditResourceType.FRIEND_LINK,
            action = AdminAuditAction.UPDATE,
            resourceId = "#p0",
            detail = "#p1.status"
    )
    public Result<FriendLinkMutationVO> updateFriendLink(
            @Positive @PathVariable Long friendLinkId,
            @Valid @RequestBody FriendLinkUpdateRequestDTO requestDTO
    ) {
        return Result.success(friendLinkService.updateFriendLink(friendLinkId, requestDTO));
    }

    /**
     * 删除友链。
     *
     * @param friendLinkId 友链 ID
     * @return 无数据
     */
    @DeleteMapping("/{friendLinkId}")
    @Operation(description = "删除友链")
    @AdminAudit(
            resourceType = AdminAuditResourceType.FRIEND_LINK,
            action = AdminAuditAction.DELETE,
            resourceId = "#p0"
    )
    public Result<Void> deleteFriendLink(@Positive @PathVariable Long friendLinkId) {
        friendLinkService.deleteFriendLink(friendLinkId);
        return Result.success(null);
    }
}
