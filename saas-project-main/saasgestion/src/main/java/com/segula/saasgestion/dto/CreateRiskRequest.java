package com.segula.saasgestion.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor
public class CreateRiskRequest {
    private Long projectId;
    // Required
    private LocalDate identificationDate;
    private String risk;
    // Optional at creation
    private String phase;
    private String category;
    private String probability;
    private String impact;
    private String managementStrategy;
    private String owner;
    private String mitigationAction;
    private BigDecimal costs;
    private BigDecimal probabilityResidual;
    private String contingencyAction;
    private String trigger;
    private String residualAction;
    private LocalDate deadline;
    private String status;
    private LocalDate closureDate;
}
