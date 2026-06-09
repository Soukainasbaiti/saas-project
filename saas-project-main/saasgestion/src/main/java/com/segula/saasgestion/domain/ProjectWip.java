package com.segula.saasgestion.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "project_wip")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectWip {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @JsonProperty("wipId")
    @Column(name = "wip_id", length = 30)
    private String wipId;

    @Column(name = "bl_number", length = 150)
    private String blNumber;

    @Column(name = "period", length = 10)
    private String period;

    @Column(name = "business_unit", length = 30)
    private String businessUnit;

    @Column(name = "bl_days", precision = 8, scale = 2)
    private BigDecimal blDays;

    @Column(name = "bl_amount", precision = 12, scale = 2)
    private BigDecimal blAmount;

    @Column(name = "billed_days", precision = 8, scale = 2)
    private BigDecimal billedDays;

    @Column(name = "wip_days", precision = 8, scale = 2)
    private BigDecimal wipDays;

    @Column(name = "wip_amount", precision = 12, scale = 2)
    private BigDecimal wipAmount;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "imported_at")
    private LocalDateTime importedAt;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "notes", length = 500)
    private String notes;
}
