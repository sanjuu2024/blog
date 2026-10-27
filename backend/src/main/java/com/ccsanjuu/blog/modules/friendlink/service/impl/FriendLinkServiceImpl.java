package com.ccsanjuu.blog.modules.friendlink.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ccsanjuu.blog.common.api.PageResult;
import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.friendlink.mapper.FriendLinkMapper;
import com.ccsanjuu.blog.modules.friendlink.model.dto.AdminFriendLinkQueryDTO;
import com.ccsanjuu.blog.modules.friendlink.model.dto.FriendLinkUpsertRequestDTO;
import com.ccsanjuu.blog.modules.friendlink.model.dto.FriendLinkUpdateRequestDTO;
import com.ccsanjuu.blog.modules.friendlink.model.entity.FriendLink;
import com.ccsanjuu.blog.modules.friendlink.model.enums.FriendLinkSection;
import com.ccsanjuu.blog.modules.friendlink.model.enums.FriendLinkStatus;
import com.ccsanjuu.blog.modules.friendlink.model.vo.AdminFriendLinkItemVO;
import com.ccsanjuu.blog.modules.friendlink.model.vo.FriendLinkMutationVO;
import com.ccsanjuu.blog.modules.friendlink.service.FriendLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class FriendLinkServiceImpl implements FriendLinkService {

    private static final int DEFAULT_SORT_NO = 100;

    private final FriendLinkMapper friendLinkMapper;

    /**
     * 获取后台友链分页列表。
     *
     * @param queryDTO 分页及筛选条件
     * @return 友链分页结果
     */
    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminFriendLinkItemVO> getAdminFriendLinkList(AdminFriendLinkQueryDTO queryDTO) {
        Page<FriendLink> page = Page.of(queryDTO.getPageNum(), queryDTO.getPageSize());
        String keyword = StringUtils.hasText(queryDTO.getKeyword())
                ? queryDTO.getKeyword().trim().toLowerCase(Locale.ROOT)
                : null;
        friendLinkMapper.selectPage(page, new LambdaQueryWrapper<FriendLink>()
                .apply(keyword != null, "LOWER(name) LIKE {0}", "%" + keyword + "%")
                .eq(queryDTO.getSection() != null, FriendLink::getSection, queryDTO.getSection())
                .eq(queryDTO.getStatus() != null, FriendLink::getStatus, queryDTO.getStatus())
                .orderByDesc(FriendLink::getCreatedAt)
                .orderByDesc(FriendLink::getId));

        return PageResult.of(page.getTotal(), page.getCurrent(), page.getSize(),
                BeanUtil.copyToList(page.getRecords(), AdminFriendLinkItemVO.class));
    }

    /**
     * 创建友链。
     *
     * @param requestDTO 友链资料
     * @return 创建结果
     */
    @Override
    @Transactional
    public FriendLinkMutationVO createFriendLink(FriendLinkUpsertRequestDTO requestDTO) {
        FriendLink friendLink = buildFriendLink(
                requestDTO.getName(),
                requestDTO.getUrl(),
                requestDTO.getLogoUrl(),
                requestDTO.getDescription(),
                requestDTO.getSection(),
                requestDTO.getSortNo(),
                requestDTO.getStatus()
        );
        ensureUrlIsUnique(friendLink.getUrl(), null);
        try {
            friendLinkMapper.insert(friendLink);
        } catch (DuplicateKeyException exception) {
            throw new BizException(ResultCode.FRIEND_LINK_URL_ALREADY_EXISTS);
        }
        return toMutationVO(friendLinkMapper.selectById(friendLink.getId()), friendLink);
    }

    /**
     * 更新友链。
     *
     * @param friendLinkId 友链 ID
     * @param requestDTO 友链资料
     * @return 更新结果
     */
    @Override
    @Transactional
    public FriendLinkMutationVO updateFriendLink(Long friendLinkId, FriendLinkUpdateRequestDTO requestDTO) {
        getFriendLink(friendLinkId);
        FriendLink update = buildFriendLink(
                requestDTO.getName(),
                requestDTO.getUrl(),
                requestDTO.getLogoUrl(),
                requestDTO.getDescription(),
                requestDTO.getSection(),
                requestDTO.getSortNo(),
                requestDTO.getStatus()
        );
        ensureUrlIsUnique(update.getUrl(), friendLinkId);
        update.setId(friendLinkId);
        try {
            friendLinkMapper.updateById(update);
        } catch (DuplicateKeyException exception) {
            throw new BizException(ResultCode.FRIEND_LINK_URL_ALREADY_EXISTS);
        }
        return toMutationVO(friendLinkMapper.selectById(friendLinkId), update);
    }

    /**
     * 删除友链。
     *
     * @param friendLinkId 友链 ID
     */
    @Override
    @Transactional
    public void deleteFriendLink(Long friendLinkId) {
        getFriendLink(friendLinkId);
        friendLinkMapper.deleteById(friendLinkId);
    }

    /**
     * 将友链 URL 规范化为允许的 HTTP(S) 绝对地址。
     *
     * @param value 原始地址
     * @param optional 是否允许空值
     * @return 规范地址
     */
    private String normalizeHttpUrl(String value, boolean optional) {
        String url = value == null ? "" : value.trim();
        if (url.isEmpty() && optional) {
            return "";
        }
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException exception) {
            throw new BizException(ResultCode.PARAM_INVALID);
        }

        boolean allowedScheme = "http".equalsIgnoreCase(uri.getScheme())
                || "https".equalsIgnoreCase(uri.getScheme());
        if (!allowedScheme || uri.getHost() == null || uri.getUserInfo() != null) {
            throw new BizException(ResultCode.PARAM_INVALID);
        }

        String normalizedUrl = uri.toASCIIString();
        if (normalizedUrl.length() > 500) {
            throw new BizException(ResultCode.PARAM_INVALID);
        }
        return normalizedUrl;
    }

    /**
     * 组装并规范化通过字段验证的友链数据。
     *
     * @param name 网站名称
     * @param url 网站地址
     * @param logoUrl Logo 地址
     * @param description 网站描述
     * @param section 固定分区
     * @param sortNo 分区内顺序
     * @param status 启用状态
     * @return 已规范化的友链实体
     */
    private FriendLink buildFriendLink(
            String name,
            String url,
            String logoUrl,
            String description,
            FriendLinkSection section,
            Integer sortNo,
            FriendLinkStatus status
    ) {
        String normalizedUrl = normalizeHttpUrl(url, false);
        String normalizedLogoUrl = normalizeHttpUrl(logoUrl, true);
        return FriendLink.builder()
                .name(name.trim())
                .url(normalizedUrl)
                .logoUrl(normalizedLogoUrl)
                .description(description == null ? "" : description.trim())
                .section(section)
                .sortNo(sortNo == null ? DEFAULT_SORT_NO : sortNo)
                .status(status == null ? FriendLinkStatus.ENABLED : status)
                .build();
    }

    /**
     * 获取友链实体，不存在时抛出业务异常。
     *
     * @param friendLinkId 友链 ID
     * @return 友链记录
     */
    private FriendLink getFriendLink(Long friendLinkId) {
        FriendLink friendLink = friendLinkMapper.selectById(friendLinkId);
        if (friendLink == null) {
            throw new BizException(ResultCode.FRIEND_LINK_NOT_FOUND);
        }
        return friendLink;
    }

    /**
     * 校验全局地址唯一性。
     *
     * @param url 网站地址
     * @param excludedId 更新时排除的当前记录 ID
     */
    private void ensureUrlIsUnique(String url, Long excludedId) {
        boolean exists = friendLinkMapper.exists(new LambdaQueryWrapper<FriendLink>()
                .eq(FriendLink::getUrl, url)
                .ne(excludedId != null, FriendLink::getId, excludedId));
        if (exists) {
            throw new BizException(ResultCode.FRIEND_LINK_URL_ALREADY_EXISTS);
        }
    }

    /**
     * 将持久化记录映射为写操作响应。
     *
     * @param persisted 保存后的实体，查询未返回时使用 fallback
     * @param fallback 已执行写操作的实体
     * @return 友链写操作响应
     */
    private FriendLinkMutationVO toMutationVO(FriendLink persisted, FriendLink fallback) {
        return BeanUtil.copyProperties(persisted == null ? fallback : persisted, FriendLinkMutationVO.class);
    }
}
