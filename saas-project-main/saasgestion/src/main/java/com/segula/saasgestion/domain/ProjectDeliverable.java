package com.segula.saasgestion.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "project_deliverable")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectDeliverable {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "deliverable_id", length = 30)
    private String deliverableId; // e.g. D_Lot1_0001

    @Column(name = "lot_name", length = 50)
    private String lotName; // e.g. Lot1, Lot2

    @Column(name = "deliverable_name", length = 200)
    private String deliverableName;

    @Column(name = "discipline", length = 100)
    private String discipline;

    @Column(name = "owner", length = 100)
    private String owner;

    @Column(name = "planned_date")
    private LocalDate plannedDate;

    @Column(name = "delivery_date")
    private LocalDate deliveryDate;

    // TO_DO | IN_PROGRESS | SUBMITTED | APPROVED | BLOCKED
    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "planned_revenue", precision = 12, scale = 2)
    private BigDecimal plannedRevenue;

    @Column(name = "rf_revenue", precision = 12, scale = 2)
    private BigDecimal rfRevenue;

    @Column(name = "first_pass")
    private Boolean firstPass; // FTR

    // HIGH | MEDIUM | LOW
    @Column(name = "priority", length = 10)
    private String priority;

    @Column(name = "comments", length = 500)
    private String comments;
}
