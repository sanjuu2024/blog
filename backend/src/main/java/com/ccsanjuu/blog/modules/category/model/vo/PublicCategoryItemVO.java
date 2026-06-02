package com.ccsanjuu.blog.modules.category.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicCategoryItemVO {

    private Long id;

    private Long parentId;

    private String name;

    private Integer level;

    private String description;

    private Integer sortNo;

    private Long articleCount;

    private List<PublicCategoryItemVO> children;
}
