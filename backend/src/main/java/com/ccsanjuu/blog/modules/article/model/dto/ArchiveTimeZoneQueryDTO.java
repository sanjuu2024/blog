package com.ccsanjuu.blog.modules.article.model.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZoneId;

@Data
@NoArgsConstructor
public class ArchiveTimeZoneQueryDTO {

    @NotBlank(message = "浏览器时区不能为空")
    @Size(max = 100, message = "浏览器时区长度不能超过 100 个字符")
    private String timeZone;

    /**
     * 仅接受支持的命名时区，使用完整时区规则计算历史年份边界。
     *
     * @return 是否为支持的命名时区；空值交由非空约束校验
     */
    @AssertTrue(message = "浏览器时区必须是有效的 IANA 命名时区")
    public boolean isValidTimeZone() {
        return timeZone == null || timeZone.isBlank() || ZoneId.getAvailableZoneIds().contains(timeZone);
    }
}
