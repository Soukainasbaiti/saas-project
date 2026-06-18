package com.segula.saasgestion.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "project_month_status",
    uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "period"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectMonthStatus {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    /** Période au format du planning (YYYY-MM | YYYY-WXX | YYYY-MM-DD) */
    @Column(name = "period", length = 10, nullable = false)
    private String period;

    /** REAL ou FORECAST */
    @Column(name = "status", length = 10, nullable = false)
    private String status;
}
