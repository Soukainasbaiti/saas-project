package com.segula.saasgestion.dto;

import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class MonthStatusDto {
    private String month;
    private String status; // REAL | FORECAST
}
