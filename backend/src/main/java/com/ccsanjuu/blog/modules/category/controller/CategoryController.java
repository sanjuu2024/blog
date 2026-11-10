package com.ccsanjuu.blog.modules.category.controller;

import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.modules.category.model.vo.PublicCategoryItemVO;
import com.ccsanjuu.blog.modules.category.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Tag(name = "分类接口")
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 获取启用分类列表
     *
     * @return
     */
    @GetMapping
    @Operation(description = "获取启用分类列表")
    public Result<List<PublicCategoryItemVO>> getEnabledCategoryList(){
        return Result.success(categoryService.getEnabledCategoryList());
    }
}
