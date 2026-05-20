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
public class UpdatedTagVO {

    private Long id;

    private String name;

    private TagStatus status;

    private OffsetDateTime updatedAt;
}
