package com.segula.saasgestion.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class AddProjectCountryRequest {
    private Long countryId;
    private Long pmId; // nullable = "a assigner"
}
