package com.segula.saasgestion.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "project_validation_history")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectValidationHistory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "action", nullable = false, length = 20)
    private String action; // SUBMITTED / VALIDATED / REJECTED

    @Column(name = "actor_name", length = 200)
    private String actorName;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }
}
