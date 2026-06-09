package com.segula.saasgestion.dto;

import lombok.*;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor
public class CreateIssueRequest {
    private Long projectId;
    // Required
    private String issue;
    private LocalDate deadline;
    // Optional
    private String severity;
    private String priority;
    private LocalDate dte;
    private LocalDate dta;
    private String lockdown;
    private String investigation;
    private String sustainableResolution;
    private String exitCriteria;
    private String owner;
    private String qualityLeader;
    private String communication;
    private String escaladeLevel;
    private LocalDate escaladeDate;
    private String escaladeDecision;
    private String link;
    private String status;
    private LocalDate dtr;
    private String remarks;
}
