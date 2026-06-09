package com.segula.saasgestion.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "project_work_type")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectWorkType {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "name", length = 100, nullable = false)
    private String name; // ex: "Page web"

    @Column(name = "unit_label", length = 50)
    private String unitLabel; // ex: "Pages", "Rapports"

    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice; // prix par unité

    @Column(name = "planned_qty")
    private Integer plannedQty; // quantité totale prévue

    @Column(name = "duration_days")
    private Double durationDays; // durée estimée par unité (en jours)
}
