package com.segula.saasgestion.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "project_management_config")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectManagementConfig {

    @Id
    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "granularity", nullable = false, length = 20)
    @Builder.Default
    private String granularity = "MONTHLY";

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "EUR";

    // ── Validation workflow ────────────────────────────────────────
    @Column(name = "validation_status", nullable = false, length = 20)
    @Builder.Default
    private String validationStatus = "DRAFT"; // DRAFT | SUBMITTED | VALIDATED | REJECTED

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "validated_at")
    private OffsetDateTime validatedAt;

    @Column(name = "validated_by", length = 200)
    private String validatedBy;

    @Column(name = "rejection_comment", columnDefinition = "TEXT")
    private String rejectionComment;

    // ── One Pager : Health & Delivery (saisie PM) ──────────────────
    @Column(name = "delivery_confidence_level", length = 20)
    private String deliveryConfidenceLevel; // ON_TRACK | MINOR_RISKS | AT_RISK | OFF_TRACK | RECOVERY_MODE

    @Column(name = "health_score_value")
    private Integer healthScoreValue; // 0-100

    @Column(name = "health_score_status", length = 20)
    private String healthScoreStatus; // ON_TRACK | MINOR_RISKS | AT_RISK | OFF_TRACK | RECOVERY_MODE

    @Column(name = "pm_remarks", columnDefinition = "TEXT")
    private String pmRemarks;

    @Column(name = "variance_actual_comment", columnDefinition = "TEXT")
    private String varianceActualComment;

    @Column(name = "variance_trend_comment", columnDefinition = "TEXT")
    private String varianceTrendComment;

    @Column(name = "variance_landing_comment", columnDefinition = "TEXT")
    private String varianceLandingComment;

    @Column(name = "tops", columnDefinition = "TEXT")
    private String tops;

    @Column(name = "flops", columnDefinition = "TEXT")
    private String flops;
}
