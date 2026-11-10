package com.ccsanjuu.blog.modules.file.service;

import com.ccsanjuu.blog.modules.file.model.enums.AdminImageUploadScene;
import com.ccsanjuu.blog.modules.file.model.vo.UploadedImageVO;
import org.springframework.web.multipart.MultipartFile;

public interface ImageUploadService {

    /**
     * 上传当前用户头像，并预占头像上传限流额度。
     *
     * @param userId 用户 ID
     * @param file 头像文件
     * @return 上传结果
     */
    UploadedImageVO uploadAvatar(Long userId, MultipartFile file);

    /**
     * 上传后台业务图片。
     *
     * @param scene 图片使用场景
     * @param file 图片文件
     * @return 上传结果
     */
    UploadedImageVO uploadAdminImage(AdminImageUploadScene scene, MultipartFile file);
}
