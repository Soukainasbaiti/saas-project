package com.segula.saasgestion.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * Ligne de budget prévisionnel par pays (Labor Cost, matériel, licence,
 * déplacement, etc.). La somme de ces lignes alimente automatiquement
 * ProjectMonthlyForecast.cost (total projet). Catégorie libre, comme
 * ProjectOtherCost, pour rester cohérent avec l'onglet "Other Costs".
 */
@Entity @Table(name = "project_country_budget_line",
    uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "country_id", "category", "month"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectCountryBudgetLine {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;

    @Column(name = "category", length = 200, nullable = false)
    private String category;

    /** Format YYYY-MM, ex: "2026-03" (ou "0000-00" = placeholder catégorie sans saisie) */
    @Column(name = "month", length = 10, nullable = false)
    private String month;

    @Builder.Default
    @Column(name = "amount", precision = 15, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;
}
