package com.segula.saasgestion.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectCreateRequest {
    @Size(max = 30)                  private String     projectCode;
    @NotNull                         private String     frontFinancier;
    @NotNull                         private Long       projectManagerId;
    @NotBlank                        private String     buId;
    @NotNull                         private Long       customerId;
    @NotNull                         private Long       industryId;
    @NotNull                         private Long       engineeringDisciplineId;
                                     private String     functionName;
    @NotNull                         private Long       engagementId;
    @NotBlank @Size(max = 200)       private String     activity;
    @NotNull @DecimalMin("0.00")     private BigDecimal revenueBudget;
    @NotNull @DecimalMin("0.00")     private BigDecimal costBudget;
                                     private LocalDate  startDate;
                                     private LocalDate  endDate;
                                     private boolean    majorProject;
    @NotBlank                        private String     technicalOffice;
    @NotBlank                        private String     status;
                                     private Short      projectYear;
                                     private String     projectNameLegacy;
}