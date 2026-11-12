package com.ccsanjuu.blog.modules.comment.controller;

import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.modules.comment.model.vo.CommentStickerPackVO;
import com.ccsanjuu.blog.modules.comment.service.CommentStickerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/comments/stickers")
@Tag(name = "公开评论接口")
@RequiredArgsConstructor
public class CommentStickerController {

    private final CommentStickerService commentStickerService;

    /**
     * 获取预置评论表情，图片由前端静态资源提供。
     *
     * @return 按清单排列的表情库
     */
    @GetMapping
    @Operation(description = "获取预置评论表情目录")
    public Result<List<CommentStickerPackVO>> getStickers() {
        return Result.success(commentStickerService.getPacks());
    }
}
