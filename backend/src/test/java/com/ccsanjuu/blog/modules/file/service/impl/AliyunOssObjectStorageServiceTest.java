package com.ccsanjuu.blog.modules.file.service.impl;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.ccsanjuu.blog.modules.file.exception.ObjectStorageException;
import com.ccsanjuu.blog.properties.ObjectStorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AliyunOssObjectStorageServiceTest {

    @Mock
    private OSS ossClient;

    private AliyunOssObjectStorageService objectStorageService;

    @BeforeEach
    void setUp() {
        ObjectStorageProperties properties = new ObjectStorageProperties(
                "aliyun",
                "https://oss-cn-hangzhou.aliyuncs.com",
                "blog-images",
                "access-key-id",
                "access-key-secret",
                "https://img.example.com/"
        );
        objectStorageService = new AliyunOssObjectStorageService(ossClient, properties);
    }

    @Test
    void uploadShouldSetMetadataAndReturnPublicUrl() {
        byte[] content = new byte[]{1, 2, 3, 4};
        InputStream inputStream = new ByteArrayInputStream(content);

        String url = objectStorageService.upload(
                "articles/covers/cover.png",
                inputStream,
                content.length,
                "image/png"
        );

        ArgumentCaptor<ObjectMetadata> metadataCaptor = ArgumentCaptor.forClass(ObjectMetadata.class);
        verify(ossClient).putObject(
                eq("blog-images"),
                eq("articles/covers/cover.png"),
                eq(inputStream),
                metadataCaptor.capture()
        );
        ObjectMetadata metadata = metadataCaptor.getValue();
        assertEquals(content.length, metadata.getContentLength());
        assertEquals("image/png", metadata.getContentType());
        assertEquals("inline", metadata.getContentDisposition());
        assertEquals("public, max-age=31536000, immutable", metadata.getCacheControl());
        assertEquals("https://img.example.com/articles/covers/cover.png", url);
    }

    @Test
    void uploadShouldWrapAliyunClientException() {
        when(ossClient.putObject(
                eq("blog-images"),
                eq("avatar.jpg"),
                any(InputStream.class),
                any(ObjectMetadata.class)
        ))
                .thenThrow(new ClientException("connection failed"));

        assertThrows(
                ObjectStorageException.class,
                () -> objectStorageService.upload(
                        "avatar.jpg",
                        new ByteArrayInputStream(new byte[]{1}),
                        1,
                        "image/jpeg"
                )
        );
    }
}
