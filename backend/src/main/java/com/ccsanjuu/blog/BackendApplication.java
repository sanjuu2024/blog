package com.ccsanjuu.blog;

import com.ccsanjuu.blog.common.database.DatabasePreparation;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.util.Arrays;

@SpringBootApplication
@ConfigurationPropertiesScan("com.ccsanjuu.blog.properties")
public class BackendApplication {

    public static void main(String[] args) {
        // 一次性维护直接复用 jar，不加载业务 Spring 上下文或要求 JWT、OSS 等无关配置。
        String preparation = Arrays.stream(args)
                .filter(argument -> argument.startsWith("--database-prepare="))
                .findFirst().orElse(null);
        if (preparation != null) {
            try {
                if (!preparation.equals("--database-prepare=accounts") && !preparation.equals("--database-prepare=migrate")) {
                    throw new IllegalArgumentException("database-prepare 只允许 accounts 或 migrate");
                }
                DatabasePreparation.run(System.getenv(), preparation.equals("--database-prepare=accounts"));
            } catch (RuntimeException exception) {
                // JDBC/Flyway 异常可能携带连接信息，维护入口只输出固定提示，不打印异常正文。
                System.err.println("DATABASE_PREPARATION_FAILED：请检查数据库配置、应用账号权限和迁移日志");
                System.exit(1);
            }
            return;
        }
        SpringApplication.run(BackendApplication.class, args);
    }

}
