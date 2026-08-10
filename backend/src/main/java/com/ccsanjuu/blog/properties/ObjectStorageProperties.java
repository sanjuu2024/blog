package com.ccsanjuu.blog.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "blog.object-storage")
public record ObjectStorageProperties(
        @NotBlank String provider,
        @NotBlank
        @Pattern(regexp = "^https://.+", message = "对象存储 endpoint 必须是完整的 HTTPS URL")
        String endpoint,
        @NotBlank String bucket,
        @NotBlank String accessKeyId,
        @NotBlank String accessKeySecret,
        @NotBlank
        @Pattern(regexp = "^https?://.+", message = "对象存储公开域名必须是完整的 HTTP(S) URL")
        String publicBaseUrl
) {
}
