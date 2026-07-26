package com.segula.saasgestion.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectWorkTypeDto {
    private Long id;
    private String name;
    private String unitLabel;
    private BigDecimal unitPrice;
    private Double durationDays;
    // computed depuis les tickets (le catalogue n'a pas de quantité propre)
    private Integer orderedQty;        // somme des qté de tous les tickets (commandé)
    private Integer deliveredQty;      // somme des qté des tickets DELIVERED
    private BigDecimal orderedRevenue; // unitPrice × orderedQty
    private BigDecimal actualRevenue;  // unitPrice × deliveredQty
    private Double completionRate;     // deliveredQty / orderedQty × 100
}
