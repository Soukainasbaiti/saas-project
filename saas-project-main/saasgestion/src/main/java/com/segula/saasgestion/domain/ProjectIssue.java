package com.segula.saasgestion.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "project_issue")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectIssue {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @JsonProperty("iId")
    @Column(name = "i_id", length = 25)
    private String iId; // e.g. I__2026_0001 or I_I52703_2026_0001

    // ── Identification ─────────────────────────────────────────────
    @Column(name = "issue", length = 2000, nullable = false)
    private String issue; // FAIT / IMPACT / CONSÉQUENCE

    @Column(name = "severity", length = 10)
    private String severity; // High | Medium | Low

    @Column(name = "priority", length = 15)
    private String priority; // P1-High | P2-Medium | P3-Low

    @Column(name = "impacts", length = 30)
    private String impacts; // auto: Critical Impact | High Impact | ...

    @Column(name = "dte")
    private LocalDate dte; // Date Taken Effect (informed)

    @Column(name = "dta")
    private LocalDate dta; // Date To Acknowledge

    // ── Resolution Plan ────────────────────────────────────────────
    @Column(name = "lockdown", length = 1000)
    private String lockdown;

    @Column(name = "investigation", length = 1000)
    private String investigation;

    @Column(name = "sustainable_resolution", length = 1000)
    private String sustainableResolution;

    @Column(name = "exit_criteria", length = 1000)
    private String exitCriteria;

    // ── Owner & Communication ──────────────────────────────────────
    @Column(name = "owner", length = 100)
    private String owner;

    @Column(name = "quality_leader", length = 100)
    private String qualityLeader;

    @Column(name = "deadline")
    private LocalDate deadline; // ETA / Deadlines — BUM peut modifier

    @Column(name = "communication", length = 1000)
    private String communication;

    // ── Escalade & Clôture ─────────────────────────────────────────
    @Column(name = "escalade_level", length = 50)
    private String escaladeLevel;

    @Column(name = "escalade_date")
    private LocalDate escaladeDate;

    @Column(name = "escalade_decision", length = 500)
    private String escaladeDecision;

    @Column(name = "link", length = 50) // N/A ou R_YYYY_XXXX
    private String link;

    @Column(name = "status", length = 20) // Open | Closed | Reopened
    private String status;

    @Column(name = "dtr")
    private LocalDate dtr; // Date Time Resolution

    // auto: DTR - DTA en jours
    @Column(name = "resolution_time")
    private Integer resolutionTime;

    @Column(name = "remarks", length = 1000)
    private String remarks;
}
