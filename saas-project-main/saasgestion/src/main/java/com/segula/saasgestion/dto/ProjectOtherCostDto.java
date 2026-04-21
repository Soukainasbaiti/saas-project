package com.segula.saasgestion.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectOtherCostDto {
    private Long id;
    private String category;
    private boolean isRebill;
    // month -> amount
    private Map<String, BigDecimal> amounts;
}
