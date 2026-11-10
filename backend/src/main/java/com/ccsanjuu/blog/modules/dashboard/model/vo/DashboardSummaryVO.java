package com.ccsanjuu.blog.modules.dashboard.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryVO {

    private Long userCount;

    private Long articleCount;

    private Long viewCount;

    private Long likeCount;

    private Long commentCount;

    private Long messageCount;
}
