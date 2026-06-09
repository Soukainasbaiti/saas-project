package com.segula.saasgestion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectRiskDto {
    private Long id;
    private Long projectId;
    @JsonProperty("rId")
    private String rId;
    // Identification
    private LocalDate identificationDate;
    private String phase;
    private String risk;
    // Categorization
    private String category;
    private String probability;
    private Integer probEval;
    private BigDecimal percentProbability;
    private String impact;
    private Integer impactEval;
    private String rating;
    private String managementStrategy;
    // Management
    private String owner;
    private String mitigationAction;
    private BigDecimal costs;
    private BigDecimal probabilityResidual;
    private BigDecimal net;
    private String contingencyAction;
    private String trigger;
    // Follow Up
    private String residualAction;
    private LocalDate deadline;
    private String status;
    private LocalDate closureDate;
}
