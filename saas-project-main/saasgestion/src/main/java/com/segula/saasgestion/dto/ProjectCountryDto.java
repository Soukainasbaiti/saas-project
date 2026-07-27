package com.segula.saasgestion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectCountryDto {
    private Long id;
    private Long countryId;
    private String countryName;
    private String countryIsoCode;
    private Long pmId;
    private String pmName;
    private String pmEmail;
    @JsonProperty("isLead")
    private boolean isLead;
    private int displayOrder;
}
