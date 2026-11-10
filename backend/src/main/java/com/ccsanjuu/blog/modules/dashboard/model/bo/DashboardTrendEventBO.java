package com.ccsanjuu.blog.modules.dashboard.model.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardTrendEventBO {

    private LocalDate eventDate;

    private String metric;

    private Long amount;
}
