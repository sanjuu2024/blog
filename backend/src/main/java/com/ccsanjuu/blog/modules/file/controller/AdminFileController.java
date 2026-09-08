package com.ccsanjuu.blog.modules.file.controller;

import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.modules.audit.annotation.AdminAudit;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditAction;
import com.ccsanjuu.blog.modules.audit.model.enums.AdminAuditResourceType;
import com.ccsanjuu.blog.modules.file.model.enums.AdminImageUploadScene;
import com.ccsanjuu.blog.modules.file.model.vo.UploadedImageVO;
import com.ccsanjuu.blog.modules.file.service.ImageUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/files")
@Tag(name = "后台文件相关接口")
@RequiredArgsConstructor
public class AdminFileController {

    private final ImageUploadService imageUploadService;

    /**
     * 上传文章封面、正文图片或项目封面。
     *
     * @param scene 图片使用场景
     * @param file 图片文件
     * @return 上传结果
     */
    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(description = "上传后台图片")
    @AdminAudit(
            resourceType = AdminAuditResourceType.FILE,
            action = AdminAuditAction.UPLOAD,
            resourceId = "#result.data.url",
            detail = "#p0"
    )
    public Result<UploadedImageVO> uploadImage(
            @RequestParam AdminImageUploadScene scene,
            @RequestPart("file") MultipartFile file
    ) {
        return Result.success(imageUploadService.uploadAdminImage(scene, file));
    }
}
