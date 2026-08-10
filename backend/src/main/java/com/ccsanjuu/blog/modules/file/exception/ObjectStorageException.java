package com.ccsanjuu.blog.modules.file.exception;

/**
 * 对象存储服务调用异常。
 */
public class ObjectStorageException extends RuntimeException {

    /**
     * 创建对象存储服务调用异常。
     *
     * @param message 异常描述
     * @param cause 原始异常
     */
    public ObjectStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
