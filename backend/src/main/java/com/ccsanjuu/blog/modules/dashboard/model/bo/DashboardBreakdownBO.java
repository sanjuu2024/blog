package com.ccsanjuu.blog.modules.dashboard.model.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardBreakdownBO {

    private String key;

    private String label;

    private Long value;
}
