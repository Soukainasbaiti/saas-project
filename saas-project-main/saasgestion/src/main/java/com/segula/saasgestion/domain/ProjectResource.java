package com.segula.saasgestion.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "project_resource")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectResource {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "matricule", length = 50)
    private String matricule;

    @Column(name = "person_name", length = 200, nullable = false)
    private String personName;

    @Column(name = "contract_type", length = 50)
    private String contractType;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}
