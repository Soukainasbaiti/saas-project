package com.segula.saasgestion.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectWorkTicketDto {
    private Long id;
    private String ticketId;
    private Long workTypeId;
    private String workTypeName;
    private String unitLabel;
    private Integer quantity;
    private String consultant;
    private LocalDate assignedDate;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate deliveryDate;
    private Boolean firstPass;
    private String onTime;   // OTD | OVERDUE | PENDING
    private String status;
    private String comments;
    private BigDecimal revenue; // quantity × unitPrice
}
