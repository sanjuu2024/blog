package com.ccsanjuu.blog.common.exception;

import com.ccsanjuu.blog.common.api.Result;
import com.ccsanjuu.blog.common.api.ResultCode;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice   // 作为全局异常处理器生效
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获并处理业务异常
     * @param e 业务异常
     * @return 返回响应
     */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBizException(BizException e) {
        String message = StringUtils.hasText(e.getMessage())
                ? e.getMessage()
                : e.getResultCode().getMessage();
        log.warn("业务异常 code={}, message={}", e.getResultCode().getCode(), message);
        return Result.fail(e.getResultCode(), message);
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    public Result<List<FieldValidationError>> handleValidationException(Exception e) {
        List<FieldValidationError> fieldErrors = resolveFieldErrors(e);
        log.warn("参数校验失败，errors={}", fieldErrors);
        return Result.<List<FieldValidationError>>builder()
                .code(ResultCode.PARAM_INVALID.getCode())
                .message(ResultCode.PARAM_INVALID.getMessage())
                .data(fieldErrors)
                .build();
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统错误", e);
        return Result.fail(ResultCode.SYSTEM_ERROR);
    }

    private List<FieldValidationError> resolveFieldErrors(Exception e) {
        if (e instanceof MethodArgumentNotValidException ex) {
            return resolveBindingResultErrors(ex.getBindingResult());
        }
        if (e instanceof BindException ex) {
            return resolveBindingResultErrors(ex.getBindingResult());
        }
        if (e instanceof ConstraintViolationException ex) {
            return ex.getConstraintViolations().stream()
                    .map(v -> new FieldValidationError(
                            extractLastPathNode(v.getPropertyPath().toString()),
                            v.getMessage(),
                            v.getInvalidValue()))
                    .toList();
        }
        return List.of();
    }

    private List<FieldValidationError> resolveBindingResultErrors(BindingResult bindingResult) {
        List<FieldValidationError> errors = new ArrayList<>();

        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            errors.add(new FieldValidationError(
                    fieldError.getField(),
                    fieldError.getDefaultMessage(),
                    fieldError.getRejectedValue()));
        }

        for (ObjectError globalError : bindingResult.getGlobalErrors()) {
            errors.add(new FieldValidationError(
                    globalError.getObjectName(),
                    globalError.getDefaultMessage(),
                    null));
        }

        return errors;
    }

    private String extractLastPathNode(String path) {
        if (!StringUtils.hasText(path)) {
            return "unknown";
        }
        int lastDotIndex = path.lastIndexOf('.');
        return lastDotIndex >= 0 ? path.substring(lastDotIndex + 1) : path;
    }

    public record FieldValidationError(String field, String message, Object rejectedValue) {
    }
}
