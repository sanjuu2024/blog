package com.ccsanjuu.blog.modules.file.service.impl;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ObjectMetadata;
import com.ccsanjuu.blog.modules.file.exception.ObjectStorageException;
import com.ccsanjuu.blog.modules.file.service.ObjectStorageService;
import com.ccsanjuu.blog.properties.ObjectStorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "blog.object-storage", name = "provider", havingValue = "aliyun")
public class AliyunOssObjectStorageService implements ObjectStorageService {

    private static final String CACHE_CONTROL = "public, max-age=31536000, immutable";

    private final OSS ossClient;
    private final ObjectStorageProperties properties;

    /**
     * 上传一个公共可读对象到阿里云 OSS。
     *
     * @param objectKey OSS 对象 key
     * @param inputStream 文件输入流
     * @param size 文件字节数
     * @param contentType 文件 MIME 类型
     * @return 对象的公开访问 URL
     */
    @Override
    public String upload(String objectKey, InputStream inputStream, long size, String contentType) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(size);
        metadata.setContentType(contentType);
        metadata.setContentDisposition("inline");
        metadata.setCacheControl(CACHE_CONTROL);

        try {
            ossClient.putObject(properties.bucket(), objectKey, inputStream, metadata);
        } catch (OSSException | ClientException exception) {
            throw new ObjectStorageException("阿里云 OSS 上传失败", exception);
        }

        return properties.publicBaseUrl().replaceAll("/+$", "") + "/" + objectKey;
    }
}
