package com.segula.saasgestion.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectResourceDto {
    private Long id;
    private String matricule;
    private String personName;
    private String contractType;
    private boolean isActive;

    // month -> value  (for each tab)
    private Map<String, BigDecimal> dailyCosts;
    private Map<String, BigDecimal> workedDays;
    private Map<String, BigDecimal> billedDays;
    private Map<String, BigDecimal> dailyRates;
}
