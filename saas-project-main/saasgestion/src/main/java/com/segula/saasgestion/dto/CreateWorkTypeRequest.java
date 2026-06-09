package com.segula.saasgestion.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor
public class CreateWorkTypeRequest {
    private Long projectId;
    private String name;
    private String unitLabel;
    private BigDecimal unitPrice;
    private Integer plannedQty;
    private Double durationDays;
}
