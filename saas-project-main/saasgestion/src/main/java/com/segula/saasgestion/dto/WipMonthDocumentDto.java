package com.segula.saasgestion.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WipMonthDocumentDto {
    private Long id;
    private Integer year;
    private Integer month;
    private String documentType;
    private String fileName;
    private BigDecimal extractedAmount;
    private BigDecimal confirmedAmount;
    private LocalDateTime uploadedAt;
}
