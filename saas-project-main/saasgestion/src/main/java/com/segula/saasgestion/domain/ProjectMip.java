package com.segula.saasgestion.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "project_mip")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectMip {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @JsonProperty("mipId")
    @Column(name = "mip_id", length = 30)
    private String mipId; // MIP__2026_0001 or MIP_{projCode}_{year}_{seq}

    @Column(name = "identification_date")
    private LocalDate identificationDate;

    // Lever dropdown: Costs | Delivery | Price | Productivity | Resources
    @Column(name = "lever", length = 20)
    private String lever;

    // Action type dropdown : liste fixe dépendant du lever choisi
    @Column(name = "action_type", length = 100)
    private String actionType;

    // Required — Verbe d'action + action précise + objectif mesurable
    @Column(name = "action_description", length = 2000, nullable = false)
    private String actionDescription;

    @Column(name = "accountable", length = 100)
    private String accountable;

    // Yes | No
    @Column(name = "client_impact", length = 5)
    private String clientImpact;

    // BUM can modify
    @Column(name = "due_date")
    private LocalDate dueDate;

    // P1-High | P2-Medium — BUM can modify
    @Column(name = "priority", length = 15)
    private String priority;

    @Column(name = "risques_prerequis", length = 1000)
    private String risquesPrerequis;

    @Column(name = "planned_gain", precision = 12, scale = 2)
    private BigDecimal plannedGain;

    @Column(name = "realized_gain", precision = 12, scale = 2)
    private BigDecimal realizedGain;

    // Identified | In Progress | Completed | Cancelled
    @Column(name = "status", length = 20)
    private String status;
}
