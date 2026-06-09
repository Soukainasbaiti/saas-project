package com.segula.saasgestion.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "project_risk")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectRisk {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @JsonProperty("rId")
    @Column(name = "r_id", length = 20)
    private String rId; // e.g. R_2026_0001

    // ── Identification ──────────────────────────────────────────────
    @Column(name = "identification_date", nullable = false)
    private LocalDate identificationDate;

    @Column(name = "phase", length = 50)
    private String phase;

    // Risk field: "Si... Alors... Avec..." (single text, structured)
    @Column(name = "risk", length = 1000, nullable = false)
    private String risk;

    // ── Risk Categorization ────────────────────────────────────────
    @Column(name = "category", length = 50)
    private String category;

    // Probability (Very Likely, Likely, Possible, Unlikely, Very unlikely)
    @Column(name = "probability", length = 20)
    private String probability;

    // Auto: derived from probability
    @Column(name = "prob_eval")
    private Integer probEval;

    // Auto: % derived from probability
    @Column(name = "percent_probability", precision = 5, scale = 2)
    private BigDecimal percentProbability;

    // Impact (Negligible, Minor, Moderate, Significant, Severe)
    @Column(name = "impact", length = 20)
    private String impact;

    // Auto: derived from impact
    @Column(name = "impact_eval")
    private Integer impactEval;

    // Auto: from matrix (Low, Medium, High, Critical)
    @Column(name = "rating", length = 20)
    private String rating;

    // Management Strategy (Accept, Mitigate, Transfer, Eliminate, Contingency)
    @Column(name = "management_strategy", length = 20)
    private String managementStrategy;

    // ── Risk Management ────────────────────────────────────────────
    @Column(name = "owner", length = 100)
    private String owner;

    @Column(name = "mitigation_action", length = 1000)
    private String mitigationAction;

    @Column(name = "costs", precision = 12, scale = 2)
    private BigDecimal costs;

    @Column(name = "probability_residual", precision = 5, scale = 2)
    private BigDecimal probabilityResidual;

    // Auto: costs * probabilityResidual / 100
    @Column(name = "net", precision = 12, scale = 2)
    private BigDecimal net;

    @Column(name = "contingency_action", length = 1000)
    private String contingencyAction;

    @Column(name = "trigger", length = 500)
    private String trigger;

    // ── Follow Up ──────────────────────────────────────────────────
    @Column(name = "residual_action", length = 1000)
    private String residualAction;

    @Column(name = "deadline")
    private LocalDate deadline;

    // Status (Identified, Mitigation in progress, Became an issue, Closed)
    @Column(name = "status", length = 30)
    private String status;

    @Column(name = "closure_date")
    private LocalDate closureDate;
}
