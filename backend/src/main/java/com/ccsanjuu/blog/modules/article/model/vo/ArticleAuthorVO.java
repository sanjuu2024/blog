package com.ccsanjuu.blog.modules.article.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArticleAuthorVO {

    private Long id;

    private String username;

    private String nickname;

    private String avatarUrl;

    private String bio;
}
