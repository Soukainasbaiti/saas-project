package com.segula.saasgestion.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor
public class CreateMipRequest {
    private Long projectId;
    private String actionDescription; // required
    private LocalDate identificationDate;
    private String lever;
    private String accountable;
    private String clientImpact;
    private LocalDate dueDate;
    private String priority;
    private String risquesPrerequis;
    private BigDecimal plannedGain;
    private BigDecimal realizedGain;
    private String status;
}
