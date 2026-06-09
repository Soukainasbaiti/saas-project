package com.segula.saasgestion.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "project_opportunity")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectOpportunity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @JsonProperty("oId")
    @Column(name = "o_id", length = 25)
    private String oId; // O__2026_0001 or O_{projCode}_{year}_{seq}

    @Column(name = "identification_date")
    private LocalDate identificationDate;

    // Required — SI / Alors / Avec
    @Column(name = "opportunity_description", length = 2000, nullable = false)
    private String opportunityDescription;

    // Category dropdown
    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "costs", precision = 12, scale = 2)
    private BigDecimal costs;

    @Column(name = "price", precision = 12, scale = 2)
    private BigDecimal price;

    // Auto: price - costs
    @Column(name = "estimated_benefit", precision = 12, scale = 2)
    private BigDecimal estimatedBenefit;

    @Column(name = "percent_new_pm")
    private Integer percentNewPm; // % New PM

    @Column(name = "action_to_be_taken", length = 1000)
    private String actionToBeTaken;

    @Column(name = "owner", length = 100)
    private String owner;

    @Column(name = "deadline", nullable = false)
    private LocalDate deadline;

    // Auto: deadline < today AND status not Won/Lost
    @Column(name = "overdue")
    private Boolean overdue;

    // No by default
    @Column(name = "on_hold")
    private Boolean onHold;

    // Open | In Progress | Won | Lost
    @Column(name = "status", length = 20)
    private String status;

    // Pending | Validated | Rejected | Deferred
    @Column(name = "copil_validation", length = 20)
    private String copilValidation;

    @Column(name = "comments", length = 1000)
    private String comments;
}
