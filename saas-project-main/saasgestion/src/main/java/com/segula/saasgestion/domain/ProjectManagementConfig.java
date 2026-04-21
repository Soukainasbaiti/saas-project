package com.segula.saasgestion.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "project_management_config")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectManagementConfig {

    @Id
    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "granularity", nullable = false, length = 20)
    @Builder.Default
    private String granularity = "MONTHLY";

    @Column(name = "currency", nullable = false, length = 3)
    @Builder.Default
    private String currency = "EUR";
}
