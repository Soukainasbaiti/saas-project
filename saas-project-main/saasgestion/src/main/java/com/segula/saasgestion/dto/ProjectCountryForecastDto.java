package com.segula.saasgestion.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.Map;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectCountryForecastDto {
    private Long countryId;
    private String countryName;
    private String countryIsoCode;
    // month -> TCV
    private Map<String, BigDecimal> amounts;
}
