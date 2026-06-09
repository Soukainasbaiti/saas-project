package com.segula.saasgestion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor
public class SaveOtherCostRequest {
    private Long projectId;
    private String category;
    private String month;
    private BigDecimal amount;
    @JsonProperty("isRebill")
    private boolean isRebill;
}
