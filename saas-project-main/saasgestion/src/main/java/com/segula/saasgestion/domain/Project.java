package com.segula.saasgestion.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity @Table(name = "project")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class Project {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_code", length = 30, unique = true)
    private String projectCode;

    @Column(name = "project_id", length = 50)
    private String projectId; // ✅ ajouté

    @Column(name = "project_name", length = 300)
    private String projectName;

    @Column(name = "project_year", nullable = false)
    private Short projectYear;

    @Column(name = "project_name_legacy", length = 300)
    private String projectNameLegacy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "front_financier_id", nullable = false)
    private FrontFinancier frontFinancier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_manager_id", nullable = false)
    private AppUser projectManager;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bu_id", nullable = false)
    private BU bu;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "industry_id", nullable = false)
    private Industry industry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "engineering_discipline_id", nullable = false)
    private EngineeringDiscipline engineeringDiscipline;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "function_id")
    private ProjectFunction function;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "engagement_id", nullable = false)
    private Engagement engagement;

    @Column(name = "activity", length = 200, nullable = false)
    private String activity;

    @Column(name = "revenue_budget", precision = 15, scale = 2, nullable = false)
    private BigDecimal revenueBudget = BigDecimal.ZERO;

    @Column(name = "cost_budget", precision = 15, scale = 2, nullable = false)
    private BigDecimal costBudget = BigDecimal.ZERO;

    @Column(name = "margin_budget", precision = 15, scale = 2, insertable = false, updatable = false)
    private BigDecimal marginBudget;

    @Column(name = "project_margin", precision = 7, scale = 6, insertable = false, updatable = false)
    private BigDecimal projectMargin;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "major_project", nullable = false)
    private boolean majorProject = false;

    @Convert(converter = TechnicalOfficeConverter.class)
    @Column(name = "technical_office", nullable = false, columnDefinition = "technical_office_type")
    private TechnicalOffice technicalOffice = TechnicalOffice.BACK_OFFICE;

    @Convert(converter = ProjectStatusConverter.class)
    @Column(name = "status", nullable = false, columnDefinition = "project_status")
    private ProjectStatus status = ProjectStatus.ON_GOING;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "created_by_id", nullable = false)
    private Long createdById;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public boolean isArchived() { return deletedAt != null; }
}