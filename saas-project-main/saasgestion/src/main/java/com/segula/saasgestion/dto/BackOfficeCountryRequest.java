package com.segula.saasgestion.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class BackOfficeCountryRequest {
    @NotNull private Long countryId;
    private Long pmId; // nullable = "a assigner" (l'Admin completera plus tard)
}
