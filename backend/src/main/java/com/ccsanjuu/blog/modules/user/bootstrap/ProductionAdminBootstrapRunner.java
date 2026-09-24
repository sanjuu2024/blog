package com.ccsanjuu.blog.modules.user.bootstrap;

import com.ccsanjuu.blog.modules.user.model.dto.AdminBootstrapRequestDTO;
import com.ccsanjuu.blog.modules.user.service.ProductionAdminBootstrapService;
import com.ccsanjuu.blog.properties.AdminBootstrapProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
@Profile("bootstrap-admin")
@RequiredArgsConstructor
public class ProductionAdminBootstrapRunner implements ApplicationRunner {

    private final AdminBootstrapProperties properties;
    private final ProductionAdminBootstrapService bootstrapService;
    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        if (!environment.acceptsProfiles(Profiles.of("prod"))) {
            throw new IllegalStateException("生产管理员初始化只能与 prod profile 一起运行");
        }

        bootstrapService.bootstrap(new AdminBootstrapRequestDTO(
                normalize(properties.getUsername()),
                normalize(properties.getNickname()),
                normalize(properties.getEmail()),
                readPassword(properties.getPasswordFile())
        ));
    }

    private String readPassword(Path passwordFile) {
        if (passwordFile == null || !Files.isRegularFile(passwordFile) || !Files.isReadable(passwordFile)) {
            throw new IllegalStateException("生产管理员初始化密码文件不存在或不可读");
        }
        try {
            String password = Files.readString(passwordFile, StandardCharsets.UTF_8);
            if (password.endsWith("\r\n")) {
                return password.substring(0, password.length() - 2);
            }
            if (password.endsWith("\n")) {
                return password.substring(0, password.length() - 1);
            }
            return password;
        } catch (IOException exception) {
            throw new IllegalStateException("读取生产管理员初始化密码文件失败", exception);
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
