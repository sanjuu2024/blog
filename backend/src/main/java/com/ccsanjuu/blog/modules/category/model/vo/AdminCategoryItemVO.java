package com.ccsanjuu.blog.modules.category.model.vo;

import com.ccsanjuu.blog.modules.category.model.enums.CategoryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminCategoryItemVO {

    private Long id;

    private Long parentId;

    private Integer level;

    private String name;

    private String description;

    private Integer sortNo;

    private CategoryStatus status;

    private Integer articleCount;

    private OffsetDateTime createdAt;

    private List<AdminCategoryItemVO> children;
}
