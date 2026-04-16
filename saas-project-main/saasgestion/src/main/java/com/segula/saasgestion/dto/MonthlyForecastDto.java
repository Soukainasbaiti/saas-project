package com.segula.saasgestion.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MonthlyForecastDto {
    private String     month;    // "2026-03"
    private BigDecimal revenue;
    private BigDecimal cost;
}
