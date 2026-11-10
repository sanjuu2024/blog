package com.ccsanjuu.blog.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 锁定部署中的凭据隔离和迁移门禁，防止配置变更把管理密码重新传入业务容器。
 */
class DatabaseDeploymentConfigTest {

    @Test
    void productionAndBootstrapShouldOnlyReceiveApplicationCredentials() throws Exception {
        Map<String, Object> services = services("compose.prod.yaml");
        Map<String, Object> backend = object(services.get("backend"));
        Map<String, Object> bootstrap = object(services("compose.bootstrap.yaml").get("admin-bootstrap"));
        for (Map<String, Object> service : List.of(backend, bootstrap)) {
            Map<String, Object> environment = object(service.get("environment"));
            assertEquals("${POSTGRES_APP_USERNAME:?POSTGRES_APP_USERNAME is required}", environment.get("POSTGRES_APP_USERNAME"));
            assertEquals("${POSTGRES_APP_PASSWORD:?POSTGRES_APP_PASSWORD is required}", environment.get("POSTGRES_APP_PASSWORD"));
            assertFalse(environment.containsKey("POSTGRES_ADMIN_PASSWORD"));
            assertFalse(environment.containsValue("${POSTGRES_ADMIN_PASSWORD:?POSTGRES_ADMIN_PASSWORD is required}"));
            assertEquals("service_completed_successfully",
                    object(object(service.get("depends_on")).get("db-prepare")).get("condition"));
        }
        Map<String, Object> preparation = object(services.get("db-prepare"));
        assertEquals(backend.get("image"), preparation.get("image"));
        assertEquals("no", preparation.get("restart"));
        assertEquals("${POSTGRES_ADMIN_PASSWORD:?POSTGRES_ADMIN_PASSWORD is required}",
                object(preparation.get("environment")).get("POSTGRES_ADMIN_PASSWORD"));
        Map<String, Object> postgresEnvironment = object(object(services.get("postgres")).get("environment"));
        assertEquals("${POSTGRES_ADMIN_USERNAME:?POSTGRES_ADMIN_USERNAME is required}", postgresEnvironment.get("POSTGRES_USER"));
        assertEquals("${POSTGRES_ADMIN_PASSWORD:?POSTGRES_ADMIN_PASSWORD is required}", postgresEnvironment.get("POSTGRES_PASSWORD"));
        assertEquals("service_healthy", object(object(preparation.get("depends_on")).get("postgres")).get("condition"));
    }

    @Test
    void acceptanceShouldPrepareTheSameDatabaseAsItsBackend() throws Exception {
        Map<String, Object> services = services("compose.acceptance.yaml");
        Object backendUrl = object(object(services.get("backend")).get("environment")).get("BLOG_DB_URL");
        Object preparationUrl = object(object(services.get("db-prepare")).get("environment")).get("BLOG_DB_URL");
        assertEquals(backendUrl, preparationUrl);
    }

    @Test
    void productionShouldDisableFlywayAndLocalProfilesShouldUseSeparateManagementConnections() throws Exception {
        var loader = new YamlPropertySourceLoader();
        var prod = loader.load("prod", new ClassPathResource("application-prod.yaml")).getFirst();
        assertEquals(false, prod.getProperty("spring.flyway.enabled"));
        for (String profile : new String[]{"dev", "test"}) {
            var source = loader.load(profile, new ClassPathResource("application-" + profile + ".yaml")).getFirst();
            String prefix = profile.equals("test") ? "POSTGRES_TEST" : "POSTGRES";
            assertEquals("${" + prefix + "_ADMIN_USERNAME}", source.getProperty("spring.flyway.user"));
            assertEquals("${" + prefix + "_ADMIN_PASSWORD}", source.getProperty("spring.flyway.password"));
        }
    }

    /**
     * 将 YAML 对象映射转换为测试使用的键值结构。
     *
     * @param value YAML 对象
     * @return 字符串键映射
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> object(Object value) {
        return (Map<String, Object>) value;
    }

    /**
     * 从仓库根目录读取 Compose 服务，不加载真实环境文件或凭据。
     *
     * @param filename Compose 文件名
     * @return 服务定义
     * @throws Exception 文件读取失败
     */
    private Map<String, Object> services(String filename) throws Exception {
        Path root = Path.of(System.getProperty("user.dir"));
        if (!Files.isRegularFile(root.resolve(filename))) {
            root = root.getParent();
        }
        try (var reader = Files.newBufferedReader(root.resolve(filename))) {
            return object(object(new Yaml().load(reader)).get("services"));
        }
    }
}
