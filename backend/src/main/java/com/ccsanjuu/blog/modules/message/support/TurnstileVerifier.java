package com.ccsanjuu.blog.modules.message.support;

import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.message.model.bo.TurnstileVerificationBO;
import com.ccsanjuu.blog.properties.TurnstileProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class TurnstileVerifier {

    private final RestClient restClient;
    private final TurnstileProperties properties;

    /**
     * 创建游客留言 Turnstile 验证器。
     *
     * @param restClient Turnstile HTTP 客户端
     * @param properties Turnstile 配置
     */
    public TurnstileVerifier(
            @Qualifier("turnstileRestClient") RestClient restClient,
            TurnstileProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    /**
     * 校验游客留言的人机验证 token。
     *
     * @param token Turnstile 客户端 token
     * @param clientIp 客户端 IP
     */
    public void verify(String token, String clientIp) {
        if (!properties.isEnabled()) {
            throw new BizException(ResultCode.TURNSTILE_SERVICE_UNAVAILABLE);
        }
        if (!StringUtils.hasText(token)) {
            throw new BizException(ResultCode.TURNSTILE_VERIFICATION_FAILED);
        }

        MultiValueMap<String, String> form = new SensitiveForm();
        form.add("secret", properties.getSecretKey());
        form.add("response", token);
        if (StringUtils.hasText(clientIp)) {
            form.add("remoteip", clientIp);
        }

        TurnstileVerificationBO response;
        try {
            response = restClient.post()
                    .uri("/turnstile/v0/siteverify")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(TurnstileVerificationBO.class);
        } catch (RuntimeException exception) {
            log.warn("turnstile_event=VERIFY_FAILED outcome=FAIL reason=SERVICE_UNAVAILABLE");
            throw new BizException(ResultCode.TURNSTILE_SERVICE_UNAVAILABLE);
        }

        if (response == null
                || !response.isSuccess()
                || !matches(response.getHostname(), properties.getExpectedHostname())
                || !matches(response.getAction(), properties.getExpectedAction())) {
            throw new BizException(ResultCode.TURNSTILE_VERIFICATION_FAILED);
        }
    }

    private boolean matches(String actual, String expected) {
        return !StringUtils.hasText(expected) || expected.equalsIgnoreCase(actual);
    }

    /**
     * 避免 Spring HTTP 客户端在 DEBUG 日志中输出验证密钥和用户 token。
     */
    private static class SensitiveForm extends LinkedMultiValueMap<String, String> {

        @Override
        public String toString() {
            return "[REDACTED]";
        }
    }
}
