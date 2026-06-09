package com.segula.saasgestion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProjectWipDto {

    private Long id;

    @JsonProperty("wipId")
    private String wipId;

    private Long projectId;
    private String blNumber;
    private String period;
    private String businessUnit;
    private BigDecimal blDays;
    private BigDecimal blAmount;
    private BigDecimal billedDays;
    private BigDecimal wipDays;
    private BigDecimal wipAmount;
    private String fileName;
    private LocalDateTime importedAt;
    private String status;
    private String notes;
}
