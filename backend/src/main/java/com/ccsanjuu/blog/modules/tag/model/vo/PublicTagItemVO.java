package com.ccsanjuu.blog.modules.tag.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicTagItemVO {

    private Long id;

    private String name;

    private Long articleCount;
}
