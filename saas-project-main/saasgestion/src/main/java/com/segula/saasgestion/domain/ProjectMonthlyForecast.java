package com.segula.saasgestion.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "project_monthly_forecast")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectMonthlyForecast {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    /** Format YYYY-MM, ex: "2026-03" */
    @Column(name = "month", length = 7, nullable = false)
    private String month;

    @Column(name = "revenue", precision = 15, scale = 2)
    private BigDecimal revenue;

    @Column(name = "cost", precision = 15, scale = 2)
    private BigDecimal cost;

    /** COV : Client Order Value (montant de la commande client pour ce mois) */
    @Column(name = "cov", precision = 15, scale = 2)
    private BigDecimal cov;
}
