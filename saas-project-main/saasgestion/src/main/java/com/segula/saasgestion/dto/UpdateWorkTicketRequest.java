package com.segula.saasgestion.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor
public class UpdateWorkTicketRequest {
    private Integer quantity;
    private String consultant;
    private LocalDate assignedDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate deliveryDate;
    @JsonProperty("firstPass")
    private Boolean firstPass;
    private String status;
    private String comments;
}
