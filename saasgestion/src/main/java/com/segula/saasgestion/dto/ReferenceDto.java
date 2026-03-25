package com.segula.saasgestion.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

// ─── ReferenceDto ─────────────────────────────────────────────────────────────
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReferenceDto {
    private Object id;
    private String label;
    private String code;
 
}