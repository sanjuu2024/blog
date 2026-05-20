package com.ccsanjuu.blog.modules.tag.model.vo;

import com.ccsanjuu.blog.modules.tag.model.enums.TagStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTagItemVO {

    private Long id;

    private String name;

    private String description;

    private TagStatus status;

    private Integer articleCount;

    private OffsetDateTime createdAt;
}
