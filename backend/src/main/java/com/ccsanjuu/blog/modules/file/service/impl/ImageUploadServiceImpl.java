package com.ccsanjuu.blog.modules.file.service.impl;

import cn.hutool.core.io.FileTypeUtil;
import cn.hutool.core.io.IORuntimeException;
import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.file.exception.ObjectStorageException;
import com.ccsanjuu.blog.modules.file.model.enums.AdminImageUploadScene;
import com.ccsanjuu.blog.modules.file.model.enums.ImageFileType;
import com.ccsanjuu.blog.modules.file.model.vo.UploadedImageVO;
import com.ccsanjuu.blog.modules.file.service.ImageUploadService;
import com.ccsanjuu.blog.modules.file.service.ObjectStorageService;
import com.ccsanjuu.blog.modules.file.support.AvatarUploadRateLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageUploadServiceImpl implements ImageUploadService {

    private static final long AVATAR_MAX_SIZE = 2L * 1024 * 1024;
    private static final long ADMIN_IMAGE_MAX_SIZE = 10L * 1024 * 1024;

    private final ObjectStorageService objectStorageService;
    private final AvatarUploadRateLimiter avatarUploadRateLimiter;

    /**
     * 校验并上传当前用户头像。
     *
     * @param userId 用户 ID
     * @param file 头像图片
     * @return 上传结果
     */
    @Override
    public UploadedImageVO uploadAvatar(Long userId, MultipartFile file) {
        ImageFileType fileType = validateImage(file, AVATAR_MAX_SIZE);
        avatarUploadRateLimiter.acquire(userId);
        String objectKey = buildObjectKey("avatars/" + userId, fileType);
        return upload(file, fileType, objectKey, "AVATAR", userId);
    }

    /**
     * 校验并上传后台业务图片。
     *
     * @param scene 图片使用场景
     * @param file 图片文件
     * @return 上传结果
     */
    @Override
    public UploadedImageVO uploadAdminImage(AdminImageUploadScene scene, MultipartFile file) {
        ImageFileType fileType = validateImage(file, ADMIN_IMAGE_MAX_SIZE);
        String objectKey = buildObjectKey(scene.getObjectKeyPrefix(), fileType);
        return upload(file, fileType, objectKey, scene.name(), null);
    }

    /**
     * 校验图片是否存在、大小是否符合场景限制，并根据文件头识别真实类型。
     *
     * @param file 图片文件
     * @param maxSize 最大文件大小，单位为字节
     * @return 图片真实类型
     */
    private ImageFileType validateImage(MultipartFile file, long maxSize) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.IMAGE_REQUIRED);
        }
        if (file.getSize() > maxSize) {
            throw new BizException(ResultCode.IMAGE_TOO_LARGE);
        }

        try (InputStream inputStream = file.getInputStream()) {
            // 只信任文件头，不使用扩展名兜底，避免文本文件伪装成 .jpg 上传。
            String detectedType = FileTypeUtil.getType(inputStream);
            ImageFileType fileType = ImageFileType.fromDetectedType(detectedType);
            if (fileType == null) {
                throw new BizException(ResultCode.IMAGE_TYPE_NOT_SUPPORTED);
            }
            return fileType;
        } catch (IOException | IORuntimeException exception) {
            throw new BizException(ResultCode.IMAGE_TYPE_NOT_SUPPORTED);
        }
    }

    /**
     * 上传图片并封装前端需要的公开 URL 和文件元数据。
     *
     * @param file 图片文件
     * @param fileType 图片真实类型
     * @param objectKey 对象存储 key
     * @param scene 图片使用场景
     * @param userId 上传用户 ID，后台业务图片上传时为 null
     * @return 上传结果
     */
    private UploadedImageVO upload(
            MultipartFile file,
            ImageFileType fileType,
            String objectKey,
            String scene,
            Long userId
    ) {
        try (InputStream inputStream = file.getInputStream()) {
            String url = objectStorageService.upload(
                    objectKey,
                    inputStream,
                    file.getSize(),
                    fileType.getContentType()
            );
            log.info(
                    "security_event=IMAGE_UPLOAD_SUCCESS description=\"图片上传成功\" outcome=SUCCESS scene={} userId={} objectKey={} size={}",
                    scene,
                    userId,
                    objectKey,
                    file.getSize()
            );
            return UploadedImageVO.builder()
                    .url(url)
                    .originalName(resolveOriginalName(file.getOriginalFilename(), fileType))
                    .contentType(fileType.getContentType())
                    .size(file.getSize())
                    .build();
        } catch (IOException | ObjectStorageException exception) {
            log.error(
                    "security_event=IMAGE_UPLOAD_FAILED description=\"图片上传失败\" outcome=FAIL scene={} userId={} errorType={}",
                    scene,
                    userId,
                    exception.getClass().getSimpleName(),
                    exception
            );
            throw new BizException(ResultCode.IMAGE_UPLOAD_FAILED);
        }
    }

    /**
     * 按业务目录、UTC 年月和 UUID 生成不可变对象 key。
     *
     * @param prefix 对象业务目录
     * @param fileType 图片真实类型
     * @return 对象存储 key
     */
    private String buildObjectKey(String prefix, ImageFileType fileType) {
        YearMonth yearMonth = YearMonth.now(ZoneOffset.UTC);
        return prefix
                + "/" + yearMonth.getYear()
                + "/" + String.format("%02d", yearMonth.getMonthValue())
                + "/" + UUID.randomUUID().toString().replace("-", "")
                + "." + fileType.getExtension();
    }

    /**
     * 只把原始文件名用于响应展示，不参与 OSS 对象 key 生成。
     *
     * @param originalName 原始文件名
     * @param fileType 图片真实类型
     * @return 清理后的原始文件名；原始文件名为空时返回默认文件名
     */
    private String resolveOriginalName(String originalName, ImageFileType fileType) {
        String cleanedName = StringUtils.cleanPath(originalName == null ? "" : originalName);
        String filename = StringUtils.getFilename(cleanedName);
        return StringUtils.hasText(filename) ? filename : "image." + fileType.getExtension();
    }
}
