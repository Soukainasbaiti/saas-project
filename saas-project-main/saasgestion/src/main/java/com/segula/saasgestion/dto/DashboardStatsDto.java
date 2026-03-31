package com.segula.saasgestion.dto;
import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DashboardStatsDto {
    private Long       totalProjects;
    private Long       activeProjects;
    private Long       closedProjects;
    private BigDecimal totalRevenue;
    private BigDecimal totalCost;
    private BigDecimal totalMargin;
    private BigDecimal avgMarginRate;
    private Short      year;
}
