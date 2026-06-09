package com.segula.saasgestion.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wip_month_document",
       uniqueConstraints = @UniqueConstraint(columnNames = {"project_id","year","month","document_type"}))
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class WipMonthDocument {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "month", nullable = false)
    private Integer month;

    @Column(name = "document_type", length = 20, nullable = false)
    private String documentType; // BL_SIGNE | BON_COMMANDE

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "extracted_amount", precision = 15, scale = 2)
    private BigDecimal extractedAmount;

    @Column(name = "confirmed_amount", precision = 15, scale = 2)
    private BigDecimal confirmedAmount;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;
}
