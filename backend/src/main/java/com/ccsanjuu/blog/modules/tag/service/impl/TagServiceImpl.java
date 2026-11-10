package com.ccsanjuu.blog.modules.tag.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.article.mapper.ArticleTagMapper;
import com.ccsanjuu.blog.modules.article.model.entity.ArticleTag;
import com.ccsanjuu.blog.modules.category.mapper.CategoryMapper;
import com.ccsanjuu.blog.modules.category.model.entity.Category;
import com.ccsanjuu.blog.modules.category.model.enums.CategoryStatus;
import com.ccsanjuu.blog.modules.tag.mapper.TagMapper;
import com.ccsanjuu.blog.modules.article.model.bo.TagArticleCountBO;
import com.ccsanjuu.blog.modules.tag.model.dto.AdminTagQueryDTO;
import com.ccsanjuu.blog.modules.tag.model.dto.TagUpsertRequestDTO;
import com.ccsanjuu.blog.modules.tag.model.entity.Tag;
import com.ccsanjuu.blog.modules.tag.model.enums.TagStatus;
import com.ccsanjuu.blog.modules.tag.model.vo.*;
import com.ccsanjuu.blog.modules.tag.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;
    private final ArticleTagMapper articleTagMapper;
    private final CategoryMapper categoryMapper;

    /**
     * 后台获取标签列表
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

        List<Long> tagIds = tagList.stream().map(Tag::getId).toList();
        if (CollectionUtil.isEmpty(tagIds)) {
            return List.of();
        }

        Map<Long, Long> tagArticleCountMap = articleTagMapper.getArticleCountByTagIds(tagIds, null, false)
                .stream().collect(Collectors.toMap(TagArticleCountBO::getTagId, TagArticleCountBO::getArticleCount));
        res.forEach(c -> c.setArticleCount(tagArticleCountMap.getOrDefault(c.getId(),0L)));

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

        boolean hasArticle = articleTagMapper.exists(
                new LambdaQueryWrapper<ArticleTag>()
                        .eq(ArticleTag::getTagId, tagId)
        );

        if (hasArticle) {
            throw new BizException(ResultCode.TAG_HAS_ARTICLES);
        }

        tagMapper.deleteById(tagId);
    }


    /**
     * 前台获取启用标签列表
     * @return
     */
    @Override
    public List<PublicTagItemVO> getEnabledTagList() {
        // 获取所有启用标签的基础数据
        List<PublicTagItemVO> res = tagMapper.selectList(
                new LambdaQueryWrapper<Tag>()
                        .eq(Tag::getStatus, TagStatus.ENABLED)
        ).stream().map(tag -> BeanUtil.copyProperties(tag, PublicTagItemVO.class)).collect(Collectors.toList());   // 🔺注意不要直接 toList()，否则得到的是不可变（Immutable）的列表，对其 sort 会抛异常！

        if (CollectionUtil.isEmpty(res)){
            return List.of();   // 保证后面传入 getArticleCountByTagIds 的 id 数组非空
        }

        // 获取所有有效的二级分类 id（用于后续查询每个标签对应的文章数）
        // 即自己不被禁用且父分类也未被禁用的所有二级分类
        List<Category> enabledCategoryList = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getStatus, CategoryStatus.ENABLED)
        );
        Map<Long, Boolean> parentCategoryEnabled = enabledCategoryList.stream().filter(c -> c.getLevel() == 1 && c.getStatus() == CategoryStatus.ENABLED).collect(Collectors.toMap(Category::getId, c -> true));
        List<Long> validLevel2CategoryIds = enabledCategoryList.stream().filter(c -> c.getLevel() == 2 && parentCategoryEnabled.get(c.getParentId()) != null).map(Category::getId).toList();

        // 获取每一个启用的标签下的文章数
        List<TagArticleCountBO> tacList = getArticleCountByTagIds(res.stream().map(PublicTagItemVO::getId).toList(), validLevel2CategoryIds);
        Map<Long, Long> map = tacList.stream().collect(Collectors.toMap(TagArticleCountBO::getTagId, TagArticleCountBO::getArticleCount));

        res.forEach(vo -> {
            Long count = map.get(vo.getId());
            vo.setArticleCount(count == null ? 0 : count);
        });

        res.sort((a,b) -> {
            // articleCount desc, name asc, id asc
            return a.getArticleCount().equals(b.getArticleCount()) ? (a.getName().equals(b.getName()) ? a.getId().compareTo(b.getId()) : a.getName().compareTo(b.getName())) : b.getArticleCount().compareTo(a.getArticleCount());
        });

        return res;
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


    /**
     * 前台获取每一个启用的标签下的文章数
     *
     * @param tagIds                 要查询的标签 id
     * @param validLevel2CategoryIds 有效的二级分类 id
     * @return
     */
    private List<TagArticleCountBO> getArticleCountByTagIds(List<Long> tagIds, List<Long> validLevel2CategoryIds) {
        if (CollectionUtil.isEmpty(tagIds) || CollectionUtil.isEmpty(validLevel2CategoryIds)) {
            return List.of();
        }
        return articleTagMapper.getArticleCountByTagIds(tagIds, validLevel2CategoryIds, true);
    }
}
