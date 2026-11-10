package com.ccsanjuu.blog.modules.friendlink.service;

import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.modules.friendlink.model.dto.AdminFriendLinkQueryDTO;
import com.ccsanjuu.blog.modules.friendlink.model.dto.FriendLinkUpsertRequestDTO;
import com.ccsanjuu.blog.modules.friendlink.model.dto.FriendLinkUpdateRequestDTO;
import com.ccsanjuu.blog.modules.friendlink.model.vo.AdminFriendLinkItemVO;
import com.ccsanjuu.blog.modules.friendlink.model.vo.FriendLinkMutationVO;
import com.ccsanjuu.blog.modules.friendlink.model.vo.PublicFriendLinkSectionVO;
import com.ccsanjuu.blog.modules.friendlink.model.vo.PublicFriendLinkSiteProfileVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import java.util.List;

public interface FriendLinkService {

    /**
     * 获取后台友链分页列表。
     *
     * @param queryDTO 分页及筛选条件
     * @return 友链分页结果
     */
    PageResult<AdminFriendLinkItemVO> getAdminFriendLinkList(AdminFriendLinkQueryDTO queryDTO);

    /**
     * 获取固定分区顺序的启用友链。
     *
     * @return 三个固定分区及其启用友链
     */
    List<PublicFriendLinkSectionVO> getPublicFriendLinkSections();

    /**
     * 获取友链申请页展示的本站资料。
     *
     * @return 本站名称、地址、描述和 Logo
     */
    PublicFriendLinkSiteProfileVO getPublicFriendLinkSiteProfile();

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
