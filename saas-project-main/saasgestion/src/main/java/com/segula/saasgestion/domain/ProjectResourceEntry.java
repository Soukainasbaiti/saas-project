package com.segula.saasgestion.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity @Table(name = "project_resource_entry",
    uniqueConstraints = @UniqueConstraint(columnNames = {"resource_id", "month"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectResourceEntry {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resource_id", nullable = false)
    private ProjectResource resource;

    @Column(name = "month", length = 7, nullable = false)
    private String month; // YYYY-MM

    @Column(name = "daily_cost", precision = 10, scale = 2)
    private BigDecimal dailyCost = BigDecimal.ZERO;

    @Column(name = "worked_days", precision = 5, scale = 2)
    private BigDecimal workedDays = BigDecimal.ZERO;

    @Column(name = "billed_days", precision = 5, scale = 2)
    private BigDecimal billedDays = BigDecimal.ZERO;

    @Column(name = "daily_rate", precision = 10, scale = 2)
    private BigDecimal dailyRate = BigDecimal.ZERO;
}
