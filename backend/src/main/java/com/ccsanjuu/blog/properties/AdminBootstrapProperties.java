package com.ccsanjuu.blog.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@Getter
@Setter
@ConfigurationProperties(prefix = "blog.bootstrap.admin")
public class AdminBootstrapProperties {

    private String username;

    private String nickname = "站点管理员";

    private String email;

    private Path passwordFile = Path.of("/run/secrets/bootstrap_admin_password");
}
