package com.ccsanjuu.blog.modules.dashboard.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardArticleRankVO {

    private Long id;

    private String title;

    private Integer viewCount;

    private Integer likeCount;

    private Integer commentCount;
}
