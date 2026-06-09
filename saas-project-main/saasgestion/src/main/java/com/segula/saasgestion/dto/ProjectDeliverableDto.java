package com.segula.saasgestion.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectDeliverableDto {
    private Long id;
    private String deliverableId;
    private String lotName;
    private String deliverableName;
    private String discipline;
    private String owner;
    private LocalDate plannedDate;
    private LocalDate deliveryDate;
    private String status;
    private BigDecimal plannedRevenue;
    private BigDecimal rfRevenue;
    private BigDecimal gap;          // computed: rfRevenue - plannedRevenue
    private Boolean firstPass;       // FTR
    private String onTime;           // computed: OTD | OVERDUE | PENDING
    private String priority;
    private String comments;
}
