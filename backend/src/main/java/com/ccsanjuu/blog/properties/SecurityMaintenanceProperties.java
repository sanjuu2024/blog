package com.ccsanjuu.blog.properties;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "blog.security-maintenance")
public class SecurityMaintenanceProperties {

    @Min(value = 1, message = "安全事件保留天数必须大于等于 1")
    private int eventRetentionDays = 90;

    @Min(value = 1, message = "过期会话保留天数必须大于等于 1")
    private int sessionRetentionDays = 7;
}
