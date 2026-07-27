package com.segula.saasgestion.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** Pièce jointe PDF (PDCA, 8D...) rattachée à une issue. */
@Entity
@Table(name = "issue_document")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class IssueDocument {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "issue_id", nullable = false)
    private Long issueId;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;
}
