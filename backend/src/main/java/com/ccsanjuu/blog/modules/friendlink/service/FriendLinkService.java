package com.ccsanjuu.blog.modules.friendlink.service;

import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.modules.friendlink.model.dto.AdminFriendLinkQueryDTO;
import com.ccsanjuu.blog.modules.friendlink.model.dto.FriendLinkUpsertRequestDTO;
import com.ccsanjuu.blog.modules.friendlink.model.dto.FriendLinkUpdateRequestDTO;
import com.ccsanjuu.blog.modules.friendlink.model.vo.AdminFriendLinkItemVO;
import com.ccsanjuu.blog.modules.friendlink.model.vo.FriendLinkMutationVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

public interface FriendLinkService {

    /**
     * 获取后台友链分页列表。
     *
     * @param queryDTO 分页及筛选条件
     * @return 友链分页结果
     */
    PageResult<AdminFriendLinkItemVO> getAdminFriendLinkList(AdminFriendLinkQueryDTO queryDTO);

    /**
     * 创建友链。
     *
     * @param requestDTO 友链资料
     * @return 创建结果
     */
    FriendLinkMutationVO createFriendLink(@Valid FriendLinkUpsertRequestDTO requestDTO);

    /**
     * 更新友链。
     *
     * @param friendLinkId 友链 ID
     * @param requestDTO 友链资料
     * @return 更新结果
     */
    FriendLinkMutationVO updateFriendLink(@Positive Long friendLinkId,
                                          @Valid FriendLinkUpdateRequestDTO requestDTO);

    /**
     * 删除友链。
     *
     * @param friendLinkId 友链 ID
     */
    void deleteFriendLink(@Positive Long friendLinkId);
}
