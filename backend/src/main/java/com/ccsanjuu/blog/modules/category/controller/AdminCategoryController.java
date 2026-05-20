package com.ccsanjuu.blog.modules.category.controller;

import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.modules.category.model.dto.AdminCategoryQueryDTO;
import com.ccsanjuu.blog.modules.category.model.dto.CategoryUpsertRequestDTO;
import com.ccsanjuu.blog.modules.category.model.vo.AdminCategoryItemVO;
import com.ccsanjuu.blog.modules.category.model.vo.CreatedCategoryVO;
import com.ccsanjuu.blog.modules.category.model.vo.UpdatedCategoryVO;
import com.ccsanjuu.blog.modules.category.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/categories")
@Validated   // 对于简单类型参数进行校验（搭配各自的校验注释使用）
@Tag(name = "分类管理接口")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    /**
     * 获取分类列表
     * @param adminCategoryQueryDTO
     * @return
     */
    @GetMapping
    @Operation(description = "获取分类列表")
    public Result<List<AdminCategoryItemVO>> categoryPageQuery(@Valid @ModelAttribute AdminCategoryQueryDTO adminCategoryQueryDTO){
        return Result.success(categoryService.categoryPageQuery(adminCategoryQueryDTO));
    }

    /**
     * 创建分类
     * @param categoryUpsertRequestDTO
     * @return
     */
    @PostMapping
    @Operation(description = "创建分类")
    public Result<CreatedCategoryVO> createCategory(@Valid @RequestBody CategoryUpsertRequestDTO categoryUpsertRequestDTO){
        return Result.success(categoryService.createCategory(categoryUpsertRequestDTO));
    }

    /**
     * 更新分类
     * @param categoryId
     * @param categoryUpsertRequestDTO
     * @return
     */
    @PutMapping("/{categoryId}")
    @Operation(description = "更新分类")
    public Result<UpdatedCategoryVO> updateCategory(
            @Positive @PathVariable("categoryId") Long categoryId,
            @Valid @RequestBody CategoryUpsertRequestDTO categoryUpsertRequestDTO
    ){
        return Result.success(categoryService.updateCategory(categoryId, categoryUpsertRequestDTO));
    }

    /**
     * 删除分类
     * @param categoryId
     * @return
     */
    @DeleteMapping("/{categoryId}")
    @Operation(description = "删除分类")
    public Result<Void> deleteCategory(
            @Positive @PathVariable("categoryId") Long categoryId
    ){
        categoryService.deleteCategory(categoryId);
        return Result.success(null);
    }
}
