package com.ccsanjuu.blog.modules.tag.service;

import com.ccsanjuu.blog.modules.tag.model.dto.AdminTagQueryDTO;
import com.ccsanjuu.blog.modules.tag.model.dto.TagUpsertRequestDTO;
import com.ccsanjuu.blog.modules.tag.model.vo.AdminTagItemVO;
import com.ccsanjuu.blog.modules.tag.model.vo.CreatedTagVO;
import com.ccsanjuu.blog.modules.tag.model.vo.PublicTagItemVO;
import com.ccsanjuu.blog.modules.tag.model.vo.UpdatedTagVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import java.util.List;

public interface TagService {
    /**
     * 获取标签列表
     * @param adminTagQueryDTO
     * @return
     */
    List<AdminTagItemVO> getTagList(AdminTagQueryDTO adminTagQueryDTO);

    /**
     * 创建标签
     * @param tagUpsertRequestDTO
     * @return
     */
    CreatedTagVO createTag(@Valid TagUpsertRequestDTO tagUpsertRequestDTO);

    /**
     * 更新标签
     * @param tagId
     * @param tagUpsertRequestDTO
     * @return
     */
    UpdatedTagVO updateTag(@Positive Long tagId, @Valid TagUpsertRequestDTO tagUpsertRequestDTO);

    /**
     * 删除标签
     * @param tagId
     */
    void deleteTag(@Positive Long tagId);

    /**
     * 获取启用标签列表
     * @return
     */
    List<PublicTagItemVO> getEnabledTagList();
}
