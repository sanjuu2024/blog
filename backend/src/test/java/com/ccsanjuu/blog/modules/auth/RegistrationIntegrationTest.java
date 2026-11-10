package com.ccsanjuu.blog.modules.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ccsanjuu.blog.common.api.ResultCode;
import com.ccsanjuu.blog.common.exception.BizException;
import com.ccsanjuu.blog.modules.auth.model.dto.RegisterRequestDTO;
import com.ccsanjuu.blog.modules.auth.service.AuthService;
import com.ccsanjuu.blog.modules.auth.service.RegistrationEmailVerificationService;
import com.ccsanjuu.blog.modules.privacy.mapper.PrivacyPolicyVersionMapper;
import com.ccsanjuu.blog.modules.privacy.model.entity.PrivacyPolicyVersion;
import com.ccsanjuu.blog.modules.privacy.service.PrivacyPolicyService;
import com.ccsanjuu.blog.modules.user.mapper.UserMapper;
import com.ccsanjuu.blog.modules.user.model.entity.User;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "blog.mail.enabled=true",
        "blog.mail.from=notifications@sanjuu.test",
        "blog.mail.frontend-base-url=http://localhost:5173",
        "spring.mail.username=greenmail-user",
        "spring.mail.password=greenmail-password",
        "spring.mail.properties.mail.smtp.auth=false",
        "spring.mail.properties.mail.smtp.starttls.enable=false"
})
class RegistrationIntegrationTest {

    private static final Pattern CODE_PATTERN = Pattern.compile("\\b(\\d{6})\\b");
    private static final GreenMail GREEN_MAIL = new GreenMail(
            new ServerSetup(0, "127.0.0.1", ServerSetup.PROTOCOL_SMTP)
    );

    static {
        GREEN_MAIL.start();
        GREEN_MAIL.setUser("notifications@sanjuu.test", "greenmail-user", "greenmail-password");
    }

    @Autowired
    private RegistrationEmailVerificationService registrationEmailVerificationService;

    @Autowired
    private AuthService authService;

    @Autowired
    private PrivacyPolicyService privacyPolicyService;

    @Autowired
    private PrivacyPolicyVersionMapper privacyPolicyVersionMapper;

    @Autowired
    private UserMapper userMapper;

    private Long createdUserId;

    @DynamicPropertySource
    static void registerMailProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host", () -> "127.0.0.1");
        registry.add("spring.mail.port", () -> GREEN_MAIL.getSmtp().getPort());
    }

    @AfterEach
    void cleanUp() throws Exception {
        if (createdUserId != null) {
            userMapper.deleteById(createdUserId);
        }
        GREEN_MAIL.purgeEmailFromAllMailboxes();
    }

    @AfterAll
    static void stopGreenMail() {
        GREEN_MAIL.stop();
    }

    @Test
    void registrationShouldVerifyEmailAndPersistPrivacyPolicyAcceptance() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String username = "user_" + suffix;
        String email = username + "@example.test";

        registrationEmailVerificationService.sendCode(email, "192.0.2.20");
        assertTrue(GREEN_MAIL.waitForIncomingEmail(5000, 1));
        MimeMessage received = GREEN_MAIL.getReceivedMessages()[0];
        Matcher matcher = CODE_PATTERN.matcher(received.getContent().toString());
        assertTrue(matcher.find());

        String policyVersion = privacyPolicyService.getPrivacyPolicy().getVersion();
        authService.register(RegisterRequestDTO.builder()
                .username(username)
                .email(email)
                .password("Passw0rd!")
                .verificationCode(matcher.group(1))
                .privacyPolicyVersion(policyVersion)
                .build());

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email));
        assertNotNull(user);
        createdUserId = user.getId();
        assertTrue(user.getEmailVerified());
        assertNotNull(user.getEmailVerifiedAt());
        assertEquals(policyVersion, user.getPrivacyPolicyVersion());
        assertNotNull(user.getPrivacyPolicyAcceptedAt());

        PrivacyPolicyVersion snapshot = privacyPolicyVersionMapper.selectById(policyVersion);
        assertNotNull(snapshot);
        assertTrue(!snapshot.getContentMd().isBlank());
    }

    @Test
    void fifthWrongCodeShouldInvalidateCurrentVerificationCode() throws Exception {
        String email = "verify_" + UUID.randomUUID().toString().substring(0, 8) + "@example.test";
        registrationEmailVerificationService.sendCode(email, "192.0.2.21");
        assertTrue(GREEN_MAIL.waitForIncomingEmail(5000, 1));
        Matcher matcher = CODE_PATTERN.matcher(GREEN_MAIL.getReceivedMessages()[0].getContent().toString());
        assertTrue(matcher.find());
        String wrongCode = "000000".equals(matcher.group(1)) ? "999999" : "000000";

        for (int attempt = 1; attempt < 5; attempt++) {
            BizException exception = assertThrows(BizException.class,
                    () -> registrationEmailVerificationService.verifyCode(email, wrongCode));
            assertEquals(ResultCode.EMAIL_VERIFICATION_CODE_INVALID, exception.getResultCode());
        }
        BizException fifthFailure = assertThrows(BizException.class,
                () -> registrationEmailVerificationService.verifyCode(email, wrongCode));
        assertEquals(ResultCode.EMAIL_VERIFICATION_ATTEMPTS_EXCEEDED, fifthFailure.getResultCode());

        BizException invalidatedCode = assertThrows(BizException.class,
                () -> registrationEmailVerificationService.verifyCode(email, matcher.group(1)));
        assertEquals(ResultCode.EMAIL_VERIFICATION_ATTEMPTS_EXCEEDED, invalidatedCode.getResultCode());
        registrationEmailVerificationService.clearCode(email);
    }
}
