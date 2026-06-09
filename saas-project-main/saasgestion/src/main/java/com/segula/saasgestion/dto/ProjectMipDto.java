package com.segula.saasgestion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectMipDto {
    private Long id;
    private Long projectId;
    @JsonProperty("mipId")
    private String mipId;
    private LocalDate identificationDate;
    private String lever;
    private String actionDescription;
    private String accountable;
    private String clientImpact;
    private LocalDate dueDate;
    private String priority;
    private String risquesPrerequis;
    private BigDecimal plannedGain;
    private BigDecimal realizedGain;
    private String status;
}
