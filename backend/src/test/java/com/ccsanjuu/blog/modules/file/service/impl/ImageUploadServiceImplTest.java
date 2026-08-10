package com.ccsanjuu.blog.modules.file.service.impl;

import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.file.exception.ObjectStorageException;
import com.ccsanjuu.blog.modules.file.model.enums.AdminImageUploadScene;
import com.ccsanjuu.blog.modules.file.model.vo.UploadedImageVO;
import com.ccsanjuu.blog.modules.file.service.ObjectStorageService;
import com.ccsanjuu.blog.modules.file.support.AvatarUploadRateLimiter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageUploadServiceImplTest {

    private static final Long USER_ID = 10001L;

    @Mock
    private ObjectStorageService objectStorageService;

    @Mock
    private AvatarUploadRateLimiter avatarUploadRateLimiter;

    @InjectMocks
    private ImageUploadServiceImpl imageUploadService;

    @Test
    void uploadAvatarShouldValidateAndUploadJpegImage() {
        byte[] content = jpegImage();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.jpeg",
                "application/octet-stream",
                content
        );
        when(objectStorageService.upload(any(), any(), anyLong(), any()))
                .thenReturn("https://img.example.com/avatar.jpg");

        UploadedImageVO result = imageUploadService.uploadAvatar(USER_ID, file);

        ArgumentCaptor<String> objectKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(avatarUploadRateLimiter).acquire(USER_ID);
        verify(objectStorageService).upload(
                objectKeyCaptor.capture(),
                any(InputStream.class),
                eq((long) content.length),
                eq("image/jpeg")
        );
        assertTrue(objectKeyCaptor.getValue().matches(
                "avatars/10001/\\d{4}/\\d{2}/[0-9a-f]{32}\\.jpg"
        ));
        assertEquals("https://img.example.com/avatar.jpg", result.getUrl());
        assertEquals("avatar.jpeg", result.getOriginalName());
        assertEquals("image/jpeg", result.getContentType());
        assertEquals(content.length, result.getSize());
    }

    @Test
    void uploadAdminImageShouldUseSceneObjectKeyPrefix() {
        byte[] content = pngImage();
        MockMultipartFile file = new MockMultipartFile("file", "cover.png", "image/png", content);
        when(objectStorageService.upload(any(), any(), anyLong(), any()))
                .thenReturn("https://img.example.com/cover.png");

        imageUploadService.uploadAdminImage(AdminImageUploadScene.ARTICLE_COVER, file);

        ArgumentCaptor<String> objectKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(objectStorageService).upload(
                objectKeyCaptor.capture(),
                any(InputStream.class),
                eq((long) content.length),
                eq("image/png")
        );
        assertTrue(objectKeyCaptor.getValue().matches(
                "articles/covers/\\d{4}/\\d{2}/[0-9a-f]{32}\\.png"
        ));
        verifyNoInteractions(avatarUploadRateLimiter);
    }

    @Test
    void uploadAdminImageShouldAcceptWebpImageHeader() {
        byte[] content = new byte[]{
                0x52, 0x49, 0x46, 0x46,
                0x04, 0x00, 0x00, 0x00,
                0x57, 0x45, 0x42, 0x50
        };
        MockMultipartFile file = new MockMultipartFile("file", "image.webp", "image/webp", content);
        when(objectStorageService.upload(any(), any(), anyLong(), any()))
                .thenReturn("https://img.example.com/image.webp");

        imageUploadService.uploadAdminImage(AdminImageUploadScene.ARTICLE_CONTENT, file);

        verify(objectStorageService).upload(any(), any(InputStream.class), eq(12L), eq("image/webp"));
    }

    @Test
    void uploadAdminImageShouldAcceptGifImageHeader() {
        byte[] content = new byte[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61};
        MockMultipartFile file = new MockMultipartFile("file", "image.gif", "image/gif", content);
        when(objectStorageService.upload(any(), any(), anyLong(), any()))
                .thenReturn("https://img.example.com/image.gif");

        imageUploadService.uploadAdminImage(AdminImageUploadScene.ARTICLE_CONTENT, file);

        verify(objectStorageService).upload(any(), any(InputStream.class), eq(6L), eq("image/gif"));
    }

    @Test
    void uploadAvatarShouldRejectEmptyImage() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        BizException exception = assertThrows(
                BizException.class,
                () -> imageUploadService.uploadAvatar(USER_ID, file)
        );

        assertEquals(ResultCode.IMAGE_REQUIRED, exception.getResultCode());
        verifyNoInteractions(avatarUploadRateLimiter, objectStorageService);
    }

    @Test
    void uploadAvatarShouldRejectImageLargerThanTwoMegabytes() {
        MultipartFile file = largeFile(2L * 1024 * 1024 + 1);

        BizException exception = assertThrows(
                BizException.class,
                () -> imageUploadService.uploadAvatar(USER_ID, file)
        );

        assertEquals(ResultCode.IMAGE_TOO_LARGE, exception.getResultCode());
        verifyNoInteractions(avatarUploadRateLimiter, objectStorageService);
    }

    @Test
    void uploadAdminImageShouldRejectImageLargerThanTenMegabytes() {
        MultipartFile file = largeFile(10L * 1024 * 1024 + 1);

        BizException exception = assertThrows(
                BizException.class,
                () -> imageUploadService.uploadAdminImage(AdminImageUploadScene.ARTICLE_CONTENT, file)
        );

        assertEquals(ResultCode.IMAGE_TOO_LARGE, exception.getResultCode());
        verifyNoInteractions(avatarUploadRateLimiter, objectStorageService);
    }

    @Test
    void uploadAdminImageShouldRejectUnsupportedFileHeader() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fake.jpg",
                "image/jpeg",
                "not-an-image".getBytes()
        );

        BizException exception = assertThrows(
                BizException.class,
                () -> imageUploadService.uploadAdminImage(AdminImageUploadScene.ARTICLE_CONTENT, file)
        );

        assertEquals(ResultCode.IMAGE_TYPE_NOT_SUPPORTED, exception.getResultCode());
        verifyNoInteractions(avatarUploadRateLimiter, objectStorageService);
    }

    @Test
    void uploadAvatarShouldMapObjectStorageFailureToBusinessError() {
        MockMultipartFile file = new MockMultipartFile("file", "avatar.jpg", "image/jpeg", jpegImage());
        when(objectStorageService.upload(any(), any(), anyLong(), any()))
                .thenThrow(new ObjectStorageException("upload failed", new RuntimeException("test")));

        BizException exception = assertThrows(
                BizException.class,
                () -> imageUploadService.uploadAvatar(USER_ID, file)
        );

        assertEquals(ResultCode.IMAGE_UPLOAD_FAILED, exception.getResultCode());
        verify(avatarUploadRateLimiter).acquire(USER_ID);
    }

    @Test
    void uploadAvatarShouldNotAcquireRateLimitForInvalidImage() {
        MockMultipartFile file = new MockMultipartFile("file", "fake.jpg", "image/jpeg", "text".getBytes());

        assertThrows(BizException.class, () -> imageUploadService.uploadAvatar(USER_ID, file));

        verify(avatarUploadRateLimiter, never()).acquire(USER_ID);
    }

    private MultipartFile largeFile(long size) {
        MultipartFile file = org.mockito.Mockito.mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(size);
        return file;
    }

    private byte[] jpegImage() {
        return new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};
    }

    private byte[] pngImage() {
        return new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A
        };
    }
}
