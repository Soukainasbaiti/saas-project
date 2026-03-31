package com.segula.saasgestion.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity @Table(name = "project_audit_log")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectAuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "project_id", nullable = false)
    private Long projectId;
    @Column(name = "changed_by")
    private Long changedBy;
    @Column(name = "change_type", length = 10, nullable = false)
    private String changeType;
    @Column(name = "changed_at", nullable = false)
    private OffsetDateTime changedAt;
    @Column(name = "old_values", columnDefinition = "jsonb")
    private String oldValues;
    @Column(name = "new_values", columnDefinition = "jsonb")
    private String newValues;
}