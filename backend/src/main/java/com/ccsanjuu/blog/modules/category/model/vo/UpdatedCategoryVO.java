package com.ccsanjuu.blog.modules.category.model.vo;

import com.ccsanjuu.blog.modules.category.model.enums.CategoryStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatedCategoryVO {

    private Long id;

    private Long parentId;

    private Integer level;

    private String name;

    private CategoryStatus status;

    private OffsetDateTime updatedAt;
}
