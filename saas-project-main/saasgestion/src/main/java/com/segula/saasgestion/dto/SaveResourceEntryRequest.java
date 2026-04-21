package com.segula.saasgestion.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor
public class SaveResourceEntryRequest {
    private Long resourceId;
    private String month;
    private BigDecimal dailyCost;
    private BigDecimal workedDays;
    private BigDecimal billedDays;
    private BigDecimal dailyRate;
}
