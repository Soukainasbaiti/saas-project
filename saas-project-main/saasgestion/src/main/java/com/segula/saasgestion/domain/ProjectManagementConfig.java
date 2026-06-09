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
}
