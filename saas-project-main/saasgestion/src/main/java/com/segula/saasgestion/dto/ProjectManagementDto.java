package com.segula.saasgestion.dto;

import lombok.*;
import java.time.OffsetDateTime;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectManagementDto {
    private Long projectId;
    private String projectName;
    private String granularity;
    private boolean granularityLocked;
    private String currency;
    private List<String> months;
    private List<ProjectResourceDto> resources;
    private List<ProjectOtherCostDto> otherCosts;
    // ── Validation ──
    private String validationStatus;   // DRAFT | SUBMITTED | VALIDATED | REJECTED
    private String validatedBy;
    private OffsetDateTime validatedAt;
    private String rejectionComment;
    private String bumName;            // BUM name for info display
    private String engagementType;     // AT | T&M | TK | UoW | WP
    // ── Champs pour génération BL ──
    private String clientName;
    private String buTrigram;
    private String pmName;
    private String projectCode;
    private String projectBusinessId;  // ex: SMAF-PJ000023
}
