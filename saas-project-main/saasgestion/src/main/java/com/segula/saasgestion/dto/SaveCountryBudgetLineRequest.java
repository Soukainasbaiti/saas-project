package com.segula.saasgestion.dto;

import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor
public class SaveCountryBudgetLineRequest {
    private Long projectId;
    private Long countryId;
    private String category;
    private String month;
    private BigDecimal amount;
}
