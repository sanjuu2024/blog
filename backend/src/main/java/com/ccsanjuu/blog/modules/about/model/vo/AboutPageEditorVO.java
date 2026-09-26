package com.ccsanjuu.blog.modules.about.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AboutPageEditorVO {

    private Long id;

    private String username;
}
