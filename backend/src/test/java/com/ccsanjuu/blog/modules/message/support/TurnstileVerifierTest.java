package com.ccsanjuu.blog.modules.message.support;

import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.properties.TurnstileProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.APPLICATION_JSON;

class TurnstileVerifierTest {

    private TurnstileProperties properties;
    private MockRestServiceServer server;
    private TurnstileVerifier verifier;

    @BeforeEach
    void setUp() {
        properties = new TurnstileProperties();
        properties.setEnabled(true);
        properties.setSecretKey("secret");
        properties.setExpectedHostname("localhost");
        properties.setExpectedAction("guest_message");

        RestClient.Builder builder = RestClient.builder().baseUrl("https://challenges.cloudflare.com");
        server = MockRestServiceServer.bindTo(builder).build();
        verifier = new TurnstileVerifier(builder.build(), properties);
    }

    @Test
    void shouldAcceptSuccessfulVerificationWithExpectedContext() {
        server.expect(requestTo("https://challenges.cloudflare.com/turnstile/v0/siteverify"))
                .andExpect(method(POST))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(APPLICATION_JSON)
                        .body("{\"success\":true,\"hostname\":\"localhost\",\"action\":\"guest_message\"}"));

        assertDoesNotThrow(() -> verifier.verify("token", "127.0.0.1"));
        server.verify();
    }

    @Test
    void shouldAcceptOfficialTestingResponseWithoutActionWhenNotConfigured() {
        properties.setExpectedHostname("");
        properties.setExpectedAction("");
        server.expect(requestTo("https://challenges.cloudflare.com/turnstile/v0/siteverify"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(APPLICATION_JSON)
                        .body("{\"success\":true,\"hostname\":\"example.com\"}"));

        assertDoesNotThrow(() -> verifier.verify("test-token", "127.0.0.1"));
        server.verify();
    }

    @Test
    void shouldRejectFailedVerification() {
        server.expect(requestTo("https://challenges.cloudflare.com/turnstile/v0/siteverify"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(APPLICATION_JSON)
                        .body("{\"success\":false,\"error-codes\":[\"invalid-input-response\"]}"));

        BizException exception = assertThrows(BizException.class, () -> verifier.verify("token", null));

        assertEquals(ResultCode.TURNSTILE_VERIFICATION_FAILED, exception.getResultCode());
    }

    @Test
    void shouldRejectUnexpectedHostname() {
        server.expect(requestTo("https://challenges.cloudflare.com/turnstile/v0/siteverify"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(APPLICATION_JSON)
                        .body("{\"success\":true,\"hostname\":\"example.com\",\"action\":\"guest_message\"}"));

        BizException exception = assertThrows(BizException.class, () -> verifier.verify("token", null));

        assertEquals(ResultCode.TURNSTILE_VERIFICATION_FAILED, exception.getResultCode());
    }

    @Test
    void shouldRejectUnexpectedAction() {
        server.expect(requestTo("https://challenges.cloudflare.com/turnstile/v0/siteverify"))
                .andRespond(withStatus(HttpStatus.OK)
                        .contentType(APPLICATION_JSON)
                        .body("{\"success\":true,\"hostname\":\"localhost\",\"action\":\"other_action\"}"));

        BizException exception = assertThrows(BizException.class, () -> verifier.verify("token", null));

        assertEquals(ResultCode.TURNSTILE_VERIFICATION_FAILED, exception.getResultCode());
    }

    @Test
    void shouldMapUnavailableProviderToServiceError() {
        server.expect(requestTo("https://challenges.cloudflare.com/turnstile/v0/siteverify"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY));

        BizException exception = assertThrows(BizException.class, () -> verifier.verify("token", null));

        assertEquals(ResultCode.TURNSTILE_SERVICE_UNAVAILABLE, exception.getResultCode());
    }

    @Test
    void shouldRejectBlankTokenBeforeCallingProvider() {
        BizException exception = assertThrows(BizException.class, () -> verifier.verify(" ", null));

        assertEquals(ResultCode.TURNSTILE_VERIFICATION_FAILED, exception.getResultCode());
    }
}
