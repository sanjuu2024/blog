package com.ccsanjuu.blog.modules.category.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.article.mapper.ArticleMapper;
import com.ccsanjuu.blog.modules.article.model.entity.Article;
import com.ccsanjuu.blog.modules.category.mapper.CategoryMapper;
import com.ccsanjuu.blog.modules.category.model.dto.AdminCategoryQueryDTO;
import com.ccsanjuu.blog.modules.category.model.dto.CategoryUpsertRequestDTO;
import com.ccsanjuu.blog.modules.category.model.entity.Category;
import com.ccsanjuu.blog.modules.category.model.enums.CategoryStatus;
import com.ccsanjuu.blog.modules.category.model.vo.AdminCategoryItemVO;
import com.ccsanjuu.blog.modules.category.model.vo.CreatedCategoryVO;
import com.ccsanjuu.blog.modules.category.model.vo.PublicCategoryItemVO;
import com.ccsanjuu.blog.modules.category.model.vo.UpdatedCategoryVO;
import com.ccsanjuu.blog.modules.category.service.CategoryService;
import com.ccsanjuu.blog.modules.article.model.bo.CategoryArticleCountBO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryMapper categoryMapper;
    private final ArticleMapper articleMapper;

    /**
     * 后台分类管理页获取分类列表，返回规则：
     * - 不传任何筛选条件时，返回完整分类树，一级分类下包含二级分类。
     * - 传入任一筛选条件时，返回符合条件的平铺列表，每个节点的 `children` 为空数组。
     *
     * @param adminCategoryQueryDTO
     * @return
     */
    @Override
    public List<AdminCategoryItemVO> getCategoryList(AdminCategoryQueryDTO adminCategoryQueryDTO) {

        String keyword = adminCategoryQueryDTO.getKeyword();
        boolean hasKeyword = StringUtils.hasText(keyword);
        String likeKeyword = "";
        if (hasKeyword) {
            keyword = keyword.trim();
            likeKeyword = "%" + keyword.toLowerCase(Locale.ROOT) + "%";
        }

        // 拦截非法查询请求
        Integer level = adminCategoryQueryDTO.getLevel();
        Long parentId = adminCategoryQueryDTO.getParentId();
        CategoryStatus status = adminCategoryQueryDTO.getStatus();
        if (parentId != null && level != null && level == 1) {
            throw new BizException(ResultCode.PARAM_INVALID, "parentId 和 level = 1 不能同时使用。");
        }

        // 1. 查询所有符合条件的分类
        List<Category> categoryList = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .apply(hasKeyword, "LOWER(name) like {0}", likeKeyword)   // 🔺🔺🔺mysql 的 like 默认大小写不敏感，但 pg 的 like 是大小写敏感的！最好统一转化为小写查询
                        .eq(status != null, Category::getStatus, status)
                        .eq(adminCategoryQueryDTO.getLevel() != null, Category::getLevel, adminCategoryQueryDTO.getLevel())
                        .eq(adminCategoryQueryDTO.getParentId() != null, Category::getParentId, adminCategoryQueryDTO.getParentId())
                        .orderByAsc(Category::getSortNo)
                        .orderByAsc(Category::getId)
        );

        // 2. 处理 articleCount 字段
        // 获取需要查询文章数的分类 id 们（符合筛选条件的各二级分类，以及符合筛选条件的所有一级分类下的所有二级分类）
        // 符合筛选条件的所有一级分类下的所有二级分类：
        List<Long> level1CategoryIds = categoryList.stream().filter(c -> c.getLevel() == 1).map(Category::getId).toList();
        List<Category> tmp = new ArrayList<>();
        if (!CollectionUtil.isEmpty(level1CategoryIds)){
            tmp = categoryMapper.selectList(
                    new LambdaQueryWrapper<Category>()
                            .in(Category::getParentId, level1CategoryIds)
            );
        }
        List<Category> categoriesForArticleCount = Stream.concat(
                    tmp.stream(),
                    categoryList.stream().filter(c -> c.getLevel() == 2)
                ).toList();
        // 去重（其实不 override 也是一样的，查询的是一样的数据）
        categoriesForArticleCount = CollUtil.distinct(categoriesForArticleCount, Category::getId, true);

        // 获取每一个二级分类（包括启用和禁用的）下的文章数（包括所有状态的文章，不止已发布）
        Map<Long, Long> categoryArticleCountMap = getArticleCountByCategoryIds(categoriesForArticleCount.stream().map(Category::getId).toList(), false)
                .stream().collect(Collectors.toMap(CategoryArticleCountBO::getCategoryId, CategoryArticleCountBO::getArticleCount));
        // 将二级分类的文章数累加进其父分类
        categoriesForArticleCount.forEach(c -> {
            Long cur = categoryArticleCountMap.getOrDefault(c.getId(), 0L);
            Long tot = categoryArticleCountMap.get(c.getParentId());
            if (tot == null){
                tot = 0L;
            }
            tot += cur;
            categoryArticleCountMap.put(c.getParentId(), tot);
        });

        // 3. 转换为 VO 列表
        List<AdminCategoryItemVO> categoryVoList = new ArrayList<>();
        categoryList.forEach(c -> {
            AdminCategoryItemVO vo = BeanUtil.copyProperties(c, AdminCategoryItemVO.class);
            vo.setArticleCount(categoryArticleCountMap.getOrDefault(c.getId(), 0L));
            vo.setChildren(List.of());
            categoryVoList.add(vo);
        });

        // 4. 组装返回值
        List<AdminCategoryItemVO> res = new ArrayList<>();
        // 不传任何筛选条件时返回树；有筛选条件时保持平铺，避免匹配到的二级分类因父级未匹配而丢失。
        boolean hasFilter = hasKeyword || status != null || level != null || parentId != null;
        if (!hasFilter) {
            res = convertToCategoryTree(categoryVoList);
        }

        // 传入 keyword、status、parentId 或 level 时，返回符合条件的平铺列表。
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
            boolean hasArticles = articleMapper.exists(
                    new LambdaQueryWrapper<Article>()
                            .eq(Article::getCategoryId, categoryId)
            );

            if (hasArticles) {
                throw new BizException(ResultCode.CATEGORY_HAS_ARTICLES);
            }

            categoryMapper.deleteById(categoryId);
        }
    }


    /**
     * 前台获取启用分类列表
     *
     * @return
     */
    @Override
    public List<PublicCategoryItemVO> getEnabledCategoryList() {
        // 启用的所有分类
        List<Category> enabledCategoryList = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getStatus, CategoryStatus.ENABLED)
                        .orderByAsc(Category::getSortNo)
                        .orderByAsc(Category::getId)
        );
        // 启用的一级分类
        List<Category> parentCategoryList = enabledCategoryList.stream().filter(c -> c.getLevel() == 1).toList();
        // 启用的二级分类
        List<Category> childrenCategoryList = enabledCategoryList.stream().filter(c -> c.getLevel() == 2).toList();

        // 获取每一个启用的二级分类下的已发表文章数
        Map<Long, Long> categoryArticleCountMap = getArticleCountByCategoryIds(childrenCategoryList.stream().map(Category::getId).toList(), true)
                .stream().collect(Collectors.toMap(CategoryArticleCountBO::getCategoryId, CategoryArticleCountBO::getArticleCount));

        // 启用的一级分类的 map
        Map<Long, List<PublicCategoryItemVO>> childrenMap = new HashMap<>();   // children
        Map<Long, Long> articleCountMap = new HashMap<>();   // articleCount

        childrenCategoryList.forEach(c -> {
            PublicCategoryItemVO vo = BeanUtil.copyProperties(c,PublicCategoryItemVO.class);

            // 填入二级分类的 children 数组（空数组）
            vo.setChildren(List.of());

            // 填入二级分类的文章数
            Long articleCount = categoryArticleCountMap.getOrDefault(c.getId(), 0L);
            vo.setArticleCount(articleCount);

            // 累加进其父分类的总文章数
            Long tot = articleCountMap.get(c.getParentId());
            if (tot == null) {
                tot = articleCount;
            }
            else {
                tot += articleCount;
            }
            articleCountMap.put(c.getParentId(), tot);

            // 把二级分类封装成的 vo 放入其父分类的 children 列表中
            List<PublicCategoryItemVO> tmp =  childrenMap.get(c.getParentId());
            if (tmp == null){
                tmp = new ArrayList<>();
                tmp.add(vo);
                childrenMap.put(c.getParentId(), tmp);
            }
            else{
                tmp.add(vo);
            }
        });

        // 填入要返回的 children 和 总 articleCount
        List<PublicCategoryItemVO> res = BeanUtil.copyToList(parentCategoryList, PublicCategoryItemVO.class);
        res.forEach(c -> {
            c.setChildren(childrenMap.get(c.getId()) == null ? List.of() : childrenMap.get(c.getId()));
            c.setArticleCount(articleCountMap.get(c.getId()) == null ? 0L : articleCountMap.get(c.getId()));
        });

        return res;
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
                .filter(c -> c.getLevel() == 2)
                .collect(Collectors.groupingBy(AdminCategoryItemVO::getParentId));

        // 3.把二级分类放到对应的父级分类下
        for (AdminCategoryItemVO parent : res) {
            List<AdminCategoryItemVO> children = m.get(parent.getId());
            if (children != null) {
                children.forEach(c -> {
                    c.setChildren(List.of());
                });
                parent.setChildren(children);
            }
        }

        // 4. 返回结果
        return res;
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
     * 校验分类名称唯一性
     * - 一级分类查重：parentId IS NULL + name
     * - 二级分类查重：parentId = 当前父分类ID + name
     *
     * @param parentId
     * @param name
     * @param excludeCategoryId
     */
    private void checkCategoryNameUnique(Long parentId, String name, Long excludeCategoryId) {
        boolean existing = false;
        String lowerCaseName = name.trim().toLowerCase(Locale.ROOT);

        if (parentId == null) {
            // 创建 / 更新的是一级分类（所有 parentId 为 null 的分类中名称唯一）
            existing = categoryMapper.exists(
                    new LambdaQueryWrapper<Category>()
                            .isNull(Category::getParentId)
                            .apply("LOWER(name) = {0}", lowerCaseName)
                            .ne(excludeCategoryId != null, Category::getId, excludeCategoryId)
            );
        }

        else {
            // 创建 / 更新的是二级分类（同一父分类下名称唯一）
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


    /**
     * 获取给定的每个二级分类下的文章数
     *
     * @param categoryIds
     * @param published 为 true 则文章数只统计已发表文章，为 false 则文章数统计所有状态的文章
     * @return
     */
    private List<CategoryArticleCountBO> getArticleCountByCategoryIds(List<Long> categoryIds, boolean published) {
        if (CollectionUtil.isEmpty(categoryIds)){
            return List.of();
        }

        return articleMapper.getArticleCountByCategoryIds(categoryIds, published);
    }
}
