package com.ccsanjuu.blog.modules.file.service;

import java.io.InputStream;

/**
 * 对象存储适配接口，业务模块不直接依赖具体厂商 SDK。
 */
public interface ObjectStorageService {

    /**
     * 上传一个公共可读对象。
     *
     * @param objectKey OSS 对象 key
     * @param inputStream 文件输入流
     * @param size 文件字节数
     * @param contentType 文件 MIME 类型
     * @return 对象的公开访问 URL
     */
    String upload(String objectKey, InputStream inputStream, long size, String contentType);
}
