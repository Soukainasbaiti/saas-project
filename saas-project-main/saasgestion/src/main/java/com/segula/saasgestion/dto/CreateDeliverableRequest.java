package com.segula.saasgestion.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor
public class CreateDeliverableRequest {
    private Long projectId;
    private String lotName;
    private String deliverableName;
    private String discipline;
    private String owner;
    private LocalDate plannedDate;
    private BigDecimal plannedRevenue;
    private String priority;
    private String comments;
}
