package com.ccsanjuu.blog.modules.file.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadedImageVO {

    private String url;

    private String originalName;

    private String contentType;

    private Long size;
}
