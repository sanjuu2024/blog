package com.ccsanjuu.blog.modules.tag.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.tag.mapper.TagMapper;
import com.ccsanjuu.blog.modules.tag.model.dto.AdminTagQueryDTO;
import com.ccsanjuu.blog.modules.tag.model.dto.TagUpsertRequestDTO;
import com.ccsanjuu.blog.modules.tag.model.entity.Tag;
import com.ccsanjuu.blog.modules.tag.model.vo.AdminTagItemVO;
import com.ccsanjuu.blog.modules.tag.model.vo.CreatedTagVO;
import com.ccsanjuu.blog.modules.tag.model.vo.UpdatedTagVO;
import com.ccsanjuu.blog.modules.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;

    /**
     * 获取标签列表
     * @param adminTagQueryDTO
     * @return
     */
    @Override
    public List<AdminTagItemVO> getTagList(AdminTagQueryDTO adminTagQueryDTO) {
        String keyword = adminTagQueryDTO.getKeyword();
        boolean hasKeyword = StringUtils.hasText(keyword);
        String likeKeyword = "";
        if (hasKeyword) {
            keyword = keyword.trim();
            likeKeyword = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
        }

        List<Tag> tagList = tagMapper.selectList(
                new LambdaQueryWrapper<Tag>()
                        .apply(hasKeyword, "LOWER(name) like {0}", likeKeyword)   // 🔺🔺🔺mysql 的 like 默认大小写不敏感，但 pg 的 like 是大小写敏感的！最好统一转化为小写查询
                        .eq(adminTagQueryDTO.getStatus() != null, Tag::getStatus, adminTagQueryDTO.getStatus())
                        .orderByDesc(Tag::getUpdatedAt)
                        .orderByAsc(Tag::getId)
        );

        List<AdminTagItemVO> res = BeanUtil.copyToList(tagList, AdminTagItemVO.class);
        res.forEach(c -> c.setArticleCount(0));   // TODO 文章模块开发后进一步完善
        return res;
    }

    /**
     * 创建标签
     * @param tagUpsertRequestDTO
     * @return
     */
    @Override
    public CreatedTagVO createTag(TagUpsertRequestDTO tagUpsertRequestDTO) {
        // 名称唯一性校验
        checkTagNameUnique(tagUpsertRequestDTO.getName(), null);

        Tag tag = BeanUtil.copyProperties(tagUpsertRequestDTO, Tag.class);
        String name = tag.getName().trim();
        tag.setName(name);
        tagMapper.insert(tag);

        Tag newTag = tagMapper.selectById(tag.getId());   // mp 回填 id
        return BeanUtil.copyProperties(newTag == null ? tag : newTag, CreatedTagVO.class);
    }

    /**
     * 更新标签
     * @param tagId
     * @param tagUpsertRequestDTO
     * @return
     */
    @Override
    public UpdatedTagVO updateTag(Long tagId, TagUpsertRequestDTO tagUpsertRequestDTO) {
        // 该标签是否存在
        Tag current = tagMapper.selectById(tagId);
        if (current == null) {
            throw new BizException(ResultCode.TAG_NOT_FOUND);
        }

        // 名称唯一性校验
        checkTagNameUnique(tagUpsertRequestDTO.getName(), tagId);

        Tag tag = BeanUtil.copyProperties(tagUpsertRequestDTO, Tag.class);
        tag.setId(tagId);
        tag.setName(tag.getName().trim());
        tagMapper.updateById(tag);
        Tag newTag = tagMapper.selectById(tagId);
        return BeanUtil.copyProperties(newTag, UpdatedTagVO.class);
    }

    /**
     * 删除标签
     * @param tagId
     */
    @Override
    public void deleteTag(Long tagId) {
        Tag tag = tagMapper.selectById(tagId);

        if (tag == null) {
            throw new BizException(ResultCode.TAG_NOT_FOUND);
        }

        boolean hasArticle = false;
        // TODO 文章模块开发后进一步完善：查询是否有文章关联了该标签，如果有则不允许删除
        if (hasArticle) {
            throw new BizException(ResultCode.TAG_HAS_ARTICLES);
        }

        tagMapper.deleteById(tagId);
    }

    /**
     * 校验标签名称唯一性
     * @param tagName
     * @param excludeTagId
     */
    private void checkTagNameUnique(String tagName, Long excludeTagId) {
        boolean existing = tagMapper.exists(
                new LambdaQueryWrapper<Tag>()
                        .apply("LOWER(name) = {0}", tagName.trim().toLowerCase(Locale.ROOT))
                        .ne(excludeTagId != null, Tag::getId, excludeTagId)   // 更新后可以和更新前的自己名称相同
        );

        if (existing){
            throw new BizException(ResultCode.TAG_NAME_ALREADY_EXISTS);
        }
    }
}
