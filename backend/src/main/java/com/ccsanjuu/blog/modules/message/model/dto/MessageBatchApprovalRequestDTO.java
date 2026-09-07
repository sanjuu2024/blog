package com.ccsanjuu.blog.modules.message.model.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class MessageBatchApprovalRequestDTO {

    @NotEmpty(message = "留言 ID 列表不能为空")
    private List<Long> messageIds;
}
