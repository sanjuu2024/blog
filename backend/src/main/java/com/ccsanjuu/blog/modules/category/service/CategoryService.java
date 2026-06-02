package com.ccsanjuu.blog.modules.category.service;

import com.ccsanjuu.blog.modules.category.model.dto.AdminCategoryQueryDTO;
import com.ccsanjuu.blog.modules.category.model.dto.CategoryUpsertRequestDTO;
import com.ccsanjuu.blog.modules.category.model.vo.AdminCategoryItemVO;
import com.ccsanjuu.blog.modules.category.model.vo.CreatedCategoryVO;
import com.ccsanjuu.blog.modules.category.model.vo.PublicCategoryItemVO;
import com.ccsanjuu.blog.modules.category.model.vo.UpdatedCategoryVO;
import jakarta.validation.constraints.Positive;

import java.util.List;

public interface CategoryService {
    /**
     * 获取分类列表
     * @param adminCategoryQueryDTO
     * @return
     */
    List<AdminCategoryItemVO> getCategoryList(AdminCategoryQueryDTO adminCategoryQueryDTO);

    /**
     * 创建分类
     * @param categoryUpsertRequestDTO
     * @return
     */
    CreatedCategoryVO createCategory(CategoryUpsertRequestDTO categoryUpsertRequestDTO);

    /**
     * 更新分类
     * @param categoryId
     * @param categoryUpsertRequestDTO
     * @return
     */
    UpdatedCategoryVO updateCategory(Long categoryId, CategoryUpsertRequestDTO categoryUpsertRequestDTO);

    /**
     * 删除分类
     * @param categoryId
     */
    void deleteCategory(@Positive Long categoryId);

    /**
     * 获取启用分类列表
     * @return
     */
    List<PublicCategoryItemVO> getEnabledCategoryList();
}
