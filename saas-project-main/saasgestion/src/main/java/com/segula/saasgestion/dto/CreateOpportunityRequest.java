package com.segula.saasgestion.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor
public class CreateOpportunityRequest {
    private Long projectId;
    private String opportunityDescription; // required
    private LocalDate deadline;            // required
    private LocalDate identificationDate;
    private String category;
    private BigDecimal costs;
    private BigDecimal price;
    private Integer percentNewPm;
    private String actionToBeTaken;
    private String owner;
    private Boolean onHold;
    private String status;
    private String copilValidation;
    private String comments;
}
