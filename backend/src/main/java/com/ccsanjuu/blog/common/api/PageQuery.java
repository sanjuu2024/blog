package com.ccsanjuu.blog.common.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PageQuery {

    @Min(value = 1, message = "页码必须大于等于 1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页条数必须大于等于 1")
    @Max(value = 100, message = "每页条数不能超过 100")
    private Integer pageSize = 10;
}
