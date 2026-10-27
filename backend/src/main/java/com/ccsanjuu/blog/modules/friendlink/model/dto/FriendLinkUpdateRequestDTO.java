package com.ccsanjuu.blog.modules.friendlink.model.dto;

import com.ccsanjuu.blog.modules.friendlink.model.enums.FriendLinkSection;
import com.ccsanjuu.blog.modules.friendlink.model.enums.FriendLinkStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 完整更新友链的后台请求。
 */
@Data
public class FriendLinkUpdateRequestDTO {

    @NotBlank(message = "友链名称不能为空")
    @Size(max = 100, message = "友链名称长度不能超过 100 个字符")
    private String name;

    @NotBlank(message = "友链地址不能为空")
    @Size(max = 500, message = "友链地址长度不能超过 500 个字符")
    private String url;

    @Size(max = 500, message = "友链头像图片或网站代表图地址长度不能超过 500 个字符")
    private String logoUrl;

    @Size(max = 255, message = "友链描述长度不能超过 255 个字符")
    private String description;

    @NotNull(message = "友链分区不能为空")
    private FriendLinkSection section;

    @NotNull(message = "友链排序值不能为空")
    @PositiveOrZero(message = "友链排序值不能小于 0")
    private Integer sortNo;

    @NotNull(message = "友链状态不能为空")
    private FriendLinkStatus status;
}
