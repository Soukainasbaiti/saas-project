package com.segula.saasgestion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor
public class UpdateDeliverableRequest {
    private String deliverableName;
    private String discipline;
    private String owner;
    private LocalDate plannedDate;
    private LocalDate deliveryDate;
    private String status;
    private BigDecimal plannedRevenue;
    private BigDecimal rfRevenue;
    @JsonProperty("firstPass")
    private Boolean firstPass;
    private String priority;
    private String comments;
}
