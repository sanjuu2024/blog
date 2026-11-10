package com.ccsanjuu.blog.modules.category.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
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
@TableName("blog_category")
public class Category {

    private Long id;

    private String name;

    private Long parentId;

    private Integer level;

    private String description;

    private Integer sortNo;

    private CategoryStatus status;

    private String seoTitle;

    private String seoDescription;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
