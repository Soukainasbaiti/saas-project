package com.segula.saasgestion.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectWorkTypeDto {
    private Long id;
    private String name;
    private String unitLabel;
    private BigDecimal unitPrice;
    private Integer plannedQty;
    private Double durationDays;
    // computed
    private Integer deliveredQty;     // somme des qté des tickets DELIVERED
    private BigDecimal plannedRevenue; // unitPrice × plannedQty
    private BigDecimal actualRevenue;  // unitPrice × deliveredQty
    private Double completionRate;     // deliveredQty / plannedQty × 100
}
