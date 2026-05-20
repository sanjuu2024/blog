package com.ccsanjuu.blog.modules.tag.controller;

import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.modules.tag.model.dto.AdminTagQueryDTO;
import com.ccsanjuu.blog.modules.tag.model.dto.TagUpsertRequestDTO;
import com.ccsanjuu.blog.modules.tag.model.vo.AdminTagItemVO;
import com.ccsanjuu.blog.modules.tag.model.vo.CreatedTagVO;
import com.ccsanjuu.blog.modules.tag.model.vo.UpdatedTagVO;
import com.ccsanjuu.blog.modules.tag.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/tags")
@Validated
@RequiredArgsConstructor
public class AdminTagController {

    private final TagService tagService;

    /**
     * 获取标签列表
     * @param adminTagQueryDTO
     * @return
     */
    @GetMapping
    @Operation(description = "获取标签列表")
    public Result<List<AdminTagItemVO>> getTagList(@Valid @ModelAttribute AdminTagQueryDTO adminTagQueryDTO){
        return Result.success(tagService.getTagList(adminTagQueryDTO));
    }

    /**
     * 创建标签
     * @param tagUpsertRequestDTO
     * @return
     */
    @PostMapping
    @Operation(description = "创建标签")
    public Result<CreatedTagVO> createTag(@Valid @RequestBody TagUpsertRequestDTO tagUpsertRequestDTO){
        return Result.success(tagService.createTag(tagUpsertRequestDTO));
    }

    /**
     * 更新标签
     * @param tagUpsertRequestDTO
     * @return
     */
    @PutMapping("/{tagId}")
    @Operation(description = "更新标签")
    public Result<UpdatedTagVO> updateTag(
            @Positive @PathVariable Long tagId,
            @Valid @RequestBody TagUpsertRequestDTO tagUpsertRequestDTO
    ){
        return Result.success(tagService.updateTag(tagId, tagUpsertRequestDTO));
    }

    /**
     * 删除标签
     * @param tagId
     * @return
     */
    @DeleteMapping("/{tagId}")
    @Operation(description = "删除标签")
    public Result<Void> deleteTag(
            @Positive @PathVariable Long tagId
    ){
        tagService.deleteTag(tagId);
        return Result.success(null);
    }
}
