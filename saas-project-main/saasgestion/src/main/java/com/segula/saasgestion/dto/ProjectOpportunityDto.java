package com.segula.saasgestion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectOpportunityDto {
    private Long id;
    private Long projectId;
    @JsonProperty("oId")
    private String oId;
    private LocalDate identificationDate;
    private String opportunityDescription;
    private String category;
    private BigDecimal costs;
    private BigDecimal price;
    private BigDecimal estimatedBenefit;
    private Integer percentNewPm;
    private String actionToBeTaken;
    private String owner;
    private LocalDate deadline;
    private Boolean overdue;
    private Boolean onHold;
    private String status;
    private String copilValidation;
    private String comments;
}
