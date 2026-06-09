package com.segula.saasgestion.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity @Table(name = "project_other_cost",
    uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "category", "month"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectOtherCost {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "category", length = 200, nullable = false)
    private String category;

    @Column(name = "month", length = 10, nullable = false)
    private String month; // YYYY-MM | YYYY-WXX | YYYY-MM-DD

    @Builder.Default
    @Column(name = "amount", precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "is_rebill", nullable = false)
    private boolean isRebill = false;
}
