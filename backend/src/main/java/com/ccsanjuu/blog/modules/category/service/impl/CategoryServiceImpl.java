package com.ccsanjuu.blog.modules.category.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.category.mapper.CategoryMapper;
import com.ccsanjuu.blog.modules.category.model.dto.AdminCategoryQueryDTO;
import com.ccsanjuu.blog.modules.category.model.dto.CategoryUpsertRequestDTO;
import com.ccsanjuu.blog.modules.category.model.entity.Category;
import com.ccsanjuu.blog.modules.category.model.vo.AdminCategoryItemVO;
import com.ccsanjuu.blog.modules.category.model.vo.CreatedCategoryVO;
import com.ccsanjuu.blog.modules.category.model.vo.UpdatedCategoryVO;
import com.ccsanjuu.blog.modules.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;

    /**
     * 获取分类列表，返回规则：
     * - 不传 `keyword`、`level` 和 `parentId` 时，返回完整分类树，一级分类下包含二级分类。
     * - 传 `keyword` 时，返回分类名称匹配的平铺列表，每个节点的 `children` 为空数组。
     * - 传 `parentId` 时，返回该一级分类下的二级分类平铺列表，每个节点的 `children` 为空数组。
     * - 传 `level=2` 时，返回二级分类平铺列表，每个节点的 `children` 为空数组。
     * - 传 `level=1` 时，返回一级分类平铺列表，每个节点的 `children` 为空数组。
     *
     * @param adminCategoryQueryDTO
     * @return
     */
    @Override
    public List<AdminCategoryItemVO> categoryPageQuery(AdminCategoryQueryDTO adminCategoryQueryDTO) {

        String keyword = adminCategoryQueryDTO.getKeyword();
        if (StringUtils.hasText(keyword)) {
            keyword = keyword.trim();
        }
        boolean hasKeyword = StringUtils.hasText(keyword);

        // 拦截非法查询请求
        Integer level = adminCategoryQueryDTO.getLevel();
        Long parentId = adminCategoryQueryDTO.getParentId();
        if (parentId != null && level != null && level == 1) {
            throw new BizException(ResultCode.PARAM_INVALID, "parentId 和 level = 1 不能同时使用。");
        }

        // 1. 查询所有符合条件的分类
        List<Category> categoryList = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .like(hasKeyword, Category::getName, keyword)
                        .eq(adminCategoryQueryDTO.getStatus() != null, Category::getStatus, adminCategoryQueryDTO.getStatus())
                        .eq(adminCategoryQueryDTO.getLevel() != null, Category::getLevel, adminCategoryQueryDTO.getLevel())
                        .eq(adminCategoryQueryDTO.getParentId() != null, Category::getParentId, adminCategoryQueryDTO.getParentId())
                        .orderByAsc(Category::getSortNo)
                        .orderByAsc(Category::getId)
        );

        // 2. 转换为 VO 列表
        List<AdminCategoryItemVO> categoryVoList = categoryList.stream().map(c -> toAdminCategoryItemVO(c)).toList();

        // 3. 组装返回值
        List<AdminCategoryItemVO> res = new ArrayList<>();
        // (1) 不传 keyword、level、parentId：返回完整分类树，一级分类下包含二级分类。
        if (!hasKeyword && level == null && parentId == null) {
            res = convertToCategoryTree(categoryVoList);
        }

        // (2) 传 keyword 时，返回分类名称匹配的平铺列表，每个节点的 `children` 为空数组。
        // (3) 传 parentId 时，返回该一级分类下的二级分类平铺列表，每个节点的 `children` 为空数组。
        // (4) 传 level=2 时，返回二级分类平铺列表，每个节点的 `children` 为空数组。
        // (5) 传 level=1 时，返回一级分类平铺列表，每个节点的 `children` 为空数组。
        else {
            res = categoryVoList;   // 都是平铺列表
        }

        return res;
    }

    /**
     * 创建分类
     *
     * @param categoryUpsertRequestDTO
     * @return
     */
    @Override
    public CreatedCategoryVO createCategory(CategoryUpsertRequestDTO categoryUpsertRequestDTO) {
        // 合法校验
        validateCategoryParent(categoryUpsertRequestDTO.getLevel(), categoryUpsertRequestDTO.getParentId());
        checkCategoryNameUnique(categoryUpsertRequestDTO.getParentId(), categoryUpsertRequestDTO.getName(), null);

        Category category = BeanUtil.copyProperties(categoryUpsertRequestDTO, Category.class);
        categoryMapper.insert(category);
        return BeanUtil.copyProperties(category, CreatedCategoryVO.class);
    }

    /**
     * 更新分类
     *
     * @param categoryId
     * @param categoryUpsertRequestDTO
     * @return
     */
    @Override
    public UpdatedCategoryVO updateCategory(Long categoryId, CategoryUpsertRequestDTO categoryUpsertRequestDTO) {
        // 判断更新对象是否存在
        Category current = categoryMapper.selectById(categoryId);
        if (current == null) {
            throw new BizException(ResultCode.CATEGORY_NOT_FOUND);
        }

        // 不允许更新分类的级别
        if (!categoryUpsertRequestDTO.getLevel().equals(current.getLevel())) {
            throw new BizException(ResultCode.CATEGORY_UPDATE_LEVEL_NOT_ALLOWED);
        }

        // 合法校验
        validateCategoryParent(categoryUpsertRequestDTO.getLevel(), categoryUpsertRequestDTO.getParentId());
        checkCategoryNameUnique(categoryUpsertRequestDTO.getParentId(), categoryUpsertRequestDTO.getName(), categoryId);

        Category category = BeanUtil.copyProperties(categoryUpsertRequestDTO, Category.class);
        category.setId(categoryId);
        categoryMapper.updateById(category);
        Category newCategory = categoryMapper.selectById(category.getId());   // 获取更新后的分类信息
        return BeanUtil.copyProperties(newCategory, UpdatedCategoryVO.class);
    }

    /**
     * 删除分类
     *
     * @param categoryId
     */
    @Override
    public void deleteCategory(Long categoryId) {
        Category category = categoryMapper.selectById(categoryId);

        if (category == null) {
            throw new BizException(ResultCode.CATEGORY_NOT_FOUND);
        }

        // 1. 删除一级分类前，必须确认该分类下没有二级分类。
        if (category.getLevel() == 1) {
            boolean hasChildren = categoryMapper.exists(
                    new LambdaQueryWrapper<Category>()
                            .eq(Category::getParentId, categoryId)
            );

            if (hasChildren) {
                throw new BizException(ResultCode.CATEGORY_HAS_CHILDREN);
            }

            categoryMapper.deleteById(categoryId);
        }

        // 2. 删除二级分类前，必须确认该分类下没有关联文章。
        else {
            // TODO 后续文章模块开发后再完善：查询该分类下是否有文章关联
            boolean hasArticles = false;

            if (hasArticles) {
                throw new BizException(ResultCode.CATEGORY_HAS_ARTICLES);
            }

            categoryMapper.deleteById(categoryId);
        }
    }


    /**
     * 当查询所有分类时，将符合要求的所有分类转换为分类树结构返回
     * @param categoryList
     * @return
     */
    private List<AdminCategoryItemVO> convertToCategoryTree(List<AdminCategoryItemVO> categoryList){
        // 1. 提取一级分类
        List<AdminCategoryItemVO> res = new ArrayList<>();
        categoryList.forEach(c -> {
            if (c.getLevel() == 1) {
                res.add(c);
            }
        });

        // 2. 提取二级分类
        Map<Long, List<AdminCategoryItemVO>> m = categoryList.stream()
                .filter(
                        c -> {
                            if (c.getLevel() == 2)
                                return true;
                            else
                                return false;
                        })
                .collect(Collectors.groupingBy(AdminCategoryItemVO::getParentId));

        // 3.把二级分类放到对应的父级分类下
        for (AdminCategoryItemVO parent : res) {
            List<AdminCategoryItemVO> children = m.get(parent.getId());
            if (children != null) {
                List<AdminCategoryItemVO> childrenVO = BeanUtil.copyToList(children, AdminCategoryItemVO.class);
                childrenVO.forEach(c -> {
                    c.setArticleCount(0);   // TODO 后续文章模块开发后再完善
                    c.setChildren(List.of());
                });
                parent.setChildren(childrenVO);
            }
        }

        // 4. 返回结果
        return res;
    }

    /**
     * 将 Category 类转换为 AdminCategoryItemVO 类
     * @param c
     * @return
     */
    private AdminCategoryItemVO toAdminCategoryItemVO(Category c) {
        return AdminCategoryItemVO.builder()
                        .id(c.getId())
                        .parentId(c.getParentId())
                        .level(c.getLevel())
                        .name(c.getName())
                        .description(c.getDescription())
                        .sortNo(c.getSortNo())
                        .status(c.getStatus())
                        .articleCount(0)   // TODO 后续文章模块开发后再完善
                        .createdAt(c.getCreatedAt())
                        .children(List.of())
                        .build();
    }

    /**
     * 创建 / 更新分类时校验父分类的合法性
     * - 如果是一级分类，则 parentId 必须为 null
     * - 如果是二级分类，则 parentId 必须为一个的合法的一级分类
     *
     * @param level
     * @param parentId
     */
    private void validateCategoryParent(int level, Long parentId) {
        if (level == 1) {
            // 创建 / 更新的是一级分类
            // 则要求 parentId 为 null
            if (parentId != null) {
                throw new BizException(ResultCode.CATEGORY_PARENT_NOT_ALLOWED);
            }
        } else {
            // 创建 / 更新的是二级分类
            // 则要求必须有合法的父分类
            // 要求父分类：非空、存在，且为一级分类
            if (parentId == null) {
                throw new BizException(ResultCode.CATEGORY_PARENT_REQUIRED);
            }

            Category parentCategory = categoryMapper.selectById(parentId);

            if (parentCategory == null || parentCategory.getLevel() != 1) {
                throw new BizException(ResultCode.CATEGORY_PARENT_INVALID);
            }
        }
    }

    /**
     * 校验分类名字唯一性
     * - 一级分类查重：parentId IS NULL + name
     * - 二级分类查重：parentId = 当前父分类ID + name
     *
     * @param parentId
     * @param name
     * @param excludeCategoryId
     */
    private void checkCategoryNameUnique(Long parentId, String name, Long excludeCategoryId) {
        boolean existing = false;
        String lowerCaseName = name.toLowerCase();

        if (parentId == null) {
            // 创建 / 更新的是一级分类（所有 parentId 为 null 的分类中名字唯一）
            existing = categoryMapper.exists(
                    new LambdaQueryWrapper<Category>()
                            .isNull(Category::getParentId)
                            .apply("LOWER(name) = {0}", lowerCaseName)
                            .ne(excludeCategoryId != null, Category::getId, excludeCategoryId)
            );
        }

        else {
            // 创建 / 更新的是二级分类（同一父分类下名字唯一）
            existing = categoryMapper.exists(
                    new LambdaQueryWrapper<Category>()
                            .eq(Category::getParentId, parentId)
                            .apply("LOWER(name) = {0}", lowerCaseName)
                            .ne(excludeCategoryId != null, Category::getId, excludeCategoryId)
            );
        }

        if (existing) {
            throw new BizException(ResultCode.CATEGORY_NAME_ALREADY_EXISTS);
        }
    }
}
