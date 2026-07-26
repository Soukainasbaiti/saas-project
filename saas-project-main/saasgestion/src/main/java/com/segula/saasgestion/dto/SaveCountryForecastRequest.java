package com.segula.saasgestion.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor
public class SaveCountryForecastRequest {
    private Long projectId;
    private Long countryId;
    private String month;
    private BigDecimal tcv;
}
