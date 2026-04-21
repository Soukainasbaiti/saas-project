package com.segula.saasgestion.dto;

import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectManagementDto {
    private Long projectId;
    private String projectName;
    private String granularity;        // MONTHLY | WEEKLY | DAILY
    private boolean granularityLocked; // true once data has been entered
    private String currency;           // EUR | USD | MAD
    private List<String> months;       // sorted list of all periods
    private List<ProjectResourceDto> resources;
    private List<ProjectOtherCostDto> otherCosts;
}
