package com.segula.saasgestion.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectCountryBudgetLineDto {
    private Long countryId;
    private String countryName;
    private String countryIsoCode;
    private String category;
    // month -> amount
    private Map<String, BigDecimal> amounts;
}
