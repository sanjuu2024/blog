package com.ccsanjuu.blog.modules.tag.controller;

import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.modules.tag.model.vo.PublicTagItemVO;
import com.ccsanjuu.blog.modules.tag.service.TagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/tags")
@RequiredArgsConstructor
@Tag(name = "标签接口")
public class TagController {

    private final TagService tagService;

    /**
     * 获取启用标签列表
     *
     * @return
     */
    @GetMapping
    @Operation(description = "获取启用标签列表")
    public Result<List<PublicTagItemVO>> getEnabledTagList(){
        return Result.success(tagService.getEnabledTagList());
    }
}
