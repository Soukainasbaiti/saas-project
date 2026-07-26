package com.segula.saasgestion.dto;

import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectManagementDto {
    private Long projectId;
    private String projectName;
    private String granularity;
    private boolean granularityLocked;
    private String currency;
    private List<String> months;
    /** Statut Réel/Prévision par période (clé = mois, valeur = REAL | FORECAST) */
    private Map<String, String> monthStatus;
    private List<ProjectResourceDto> resources;
    private List<ProjectOtherCostDto> otherCosts;
    // ── TCV / Budget par pays (previsions) ──
    private List<ProjectCountryForecastDto> countryForecasts;
    private List<ProjectCountryBudgetLineDto> countryBudgetLines;
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
    // ── Champs pour le One Pager ──
    private LocalDate startDate;
    private LocalDate endDate;
    /** Prévisions budgétaires mensuelles (revenue/cost/cov), clé = mois */
    private Map<String, MonthlyForecastDto> monthlyForecasts;
    // ── One Pager : Health & Delivery (saisie PM) ──
    private String deliveryConfidenceLevel; // ON_TRACK | MINOR_RISKS | AT_RISK | OFF_TRACK | RECOVERY_MODE
    private Integer healthScoreValue;       // 0-100
    private String healthScoreStatus;       // ON_TRACK | MINOR_RISKS | AT_RISK | OFF_TRACK | RECOVERY_MODE
    private String pmRemarks;
    private String varianceActualComment;
    private String varianceTrendComment;
    private String varianceLandingComment;
    private String tops;
    private String flops;
}
