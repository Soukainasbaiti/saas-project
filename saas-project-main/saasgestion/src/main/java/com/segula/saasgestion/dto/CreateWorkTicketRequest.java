package com.segula.saasgestion.dto;

import lombok.*;
import java.time.LocalDate;

@Data @NoArgsConstructor @AllArgsConstructor
public class CreateWorkTicketRequest {
    private Long projectId;
    private Long workTypeId;
    private Integer quantity;
    private String consultant;
    private LocalDate assignedDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private String comments;
}
