package com.segula.saasgestion.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * TCV (revenu prévisionnel) saisi par pays et par mois. La somme de ces
 * lignes alimente automatiquement ProjectMonthlyForecast.revenue (total projet).
 */
@Entity @Table(name = "project_country_forecast",
    uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "country_id", "month"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectCountryForecast {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;

    /** Format YYYY-MM, ex: "2026-03" */
    @Column(name = "month", length = 7, nullable = false)
    private String month;

    @Builder.Default
    @Column(name = "tcv", precision = 15, scale = 2)
    private BigDecimal tcv = BigDecimal.ZERO;
}
