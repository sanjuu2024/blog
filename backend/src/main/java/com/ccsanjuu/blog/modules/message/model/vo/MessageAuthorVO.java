package com.ccsanjuu.blog.modules.message.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageAuthorVO {

    private Long id;

    private String username;

    private String nickname;

    private String avatarUrl;
}
