package com.segula.saasgestion.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;

@Entity @Table(name = "project_country")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectCountry {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "country_id", nullable = false)
    private Country country;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pm_id")
    private AppUser pm;

    @Builder.Default
    @Column(name = "is_lead", nullable = false)
    private boolean isLead = false;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private int displayOrder = 1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "added_by")
    private AppUser addedBy;

    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
