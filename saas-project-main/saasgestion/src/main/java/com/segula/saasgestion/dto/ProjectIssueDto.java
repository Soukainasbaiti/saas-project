package com.segula.saasgestion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectIssueDto {
    private Long id;
    private Long projectId;
    @JsonProperty("iId")
    private String iId;
    // Identification
    private String issue;
    private String severity;
    private String priority;
    private String impacts;
    private LocalDate dte;
    private LocalDate dta;
    // Resolution Plan
    private String lockdown;
    private String investigation;
    private String sustainableResolution;
    private String exitCriteria;
    // Owner & Communication
    private String owner;
    private String qualityLeader;
    private LocalDate deadline;
    private String communication;
    // Escalade & Clôture
    private String escaladeLevel;
    private LocalDate escaladeDate;
    private String escaladeDecision;
    private String link;
    private String status;
    private LocalDate dtr;
    private Integer resolutionTime;
    private String remarks;
    private List<IssueDocumentDto> documents;
}
