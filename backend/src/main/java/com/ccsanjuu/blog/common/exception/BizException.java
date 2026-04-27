package com.ccsanjuu.blog.common.exception;

import com.ccsanjuu.blog.common.api.ResultCode;
import lombok.Getter;

@Getter
public class BizException extends RuntimeException{

    private final ResultCode resultCode;
    private final String message;

    public BizException(ResultCode resultCode) {
        this(resultCode, null);
    }

    public BizException(ResultCode resultCode, String message) {
        super(message != null ? message : resultCode.getMessage());
        this.resultCode = resultCode;
        this.message = message;
    }
}
