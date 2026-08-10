package com.ccsanjuu.blog.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.ccsanjuu.blog.properties.ObjectStorageProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "blog.object-storage", name = "provider", havingValue = "aliyun")
public class ObjectStorageConfig {

    /**
     * 创建阿里云 OSS 客户端，应用关闭时由 Spring 调用 shutdown 释放连接资源。
     *
     * @param properties 对象存储配置
     * @return OSS 客户端
     */
    @Bean(destroyMethod = "shutdown")
    public OSS ossClient(ObjectStorageProperties properties) {
        return new OSSClientBuilder().build(
                properties.endpoint(),
                properties.accessKeyId(),
                properties.accessKeySecret()
        );
    }
}
