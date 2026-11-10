package com.ccsanjuu.blog.modules.tag.controller;

import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.modules.audit.annotation.AdminAudit;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditAction;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditResourceType;
import com.ccsanjuu.blog.modules.tag.model.dto.AdminTagQueryDTO;
import com.ccsanjuu.blog.modules.tag.model.dto.TagUpsertRequestDTO;
import com.ccsanjuu.blog.modules.tag.model.vo.AdminTagItemVO;
import com.ccsanjuu.blog.modules.tag.model.vo.CreatedTagVO;
import com.ccsanjuu.blog.modules.tag.model.vo.UpdatedTagVO;
import com.ccsanjuu.blog.modules.tag.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "标签管理接口")
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
    @AdminAudit(
            resourceType = AdminAuditResourceType.TAG,
            action = AdminAuditAction.CREATE,
            resourceId = "#result.data.id"
    )
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
    @AdminAudit(
            resourceType = AdminAuditResourceType.TAG,
            action = AdminAuditAction.UPDATE,
            resourceId = "#p0",
            detail = "#p1.status"
    )
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
    @AdminAudit(
            resourceType = AdminAuditResourceType.TAG,
            action = AdminAuditAction.DELETE,
            resourceId = "#p0"
    )
    public Result<Void> deleteTag(
            @Positive @PathVariable Long tagId
    ){
        tagService.deleteTag(tagId);
        return Result.success(null);
    }
}
