package com.ccsanjuu.blog.modules.dashboard.model.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardArticleRankBO {

    private Long id;

    private String title;

    private Integer viewCount;

    private Integer likeCount;

    private Integer commentCount;
}
