package com.segula.saasgestion.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class IssueDocumentDto {
    private Long id;
    private String fileName;
    private LocalDateTime uploadedAt;
}
