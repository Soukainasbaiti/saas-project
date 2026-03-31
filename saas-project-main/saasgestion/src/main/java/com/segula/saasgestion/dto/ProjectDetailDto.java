package com.segula.saasgestion.dto;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectDetailDto {
    private Long           id;
    private String         projectCode;
    private String         projectId; // ✅ ajouté
    private String         projectName;
    private Short          projectYear;
    private String         projectNameLegacy;
    private Long           frontFinancierId;
    private String         frontFinancier;
    private String         frontFinancierLabel;
    private Long           projectManagerId;
    private String         projectManager;
    private String         pmEmail;
    private String         buId;
    private String         buName;
    private String         buTrigram;
    private String         bumName;
    private Long           customerId;
    private String         customerName;
    private String         customerTrigram;
    private String         customerGroup;
    private Long           industryId;
    private String         industryName;
    private String         industryTrigram;
    private Long           engineeringDisciplineId;
    private String         engineeringDiscipline;
    private Long           functionId;
    private String         functionName;
    private Long           engagementId;
    private String         engagement;
    private String         engagementType;
    private String         activity;
    private boolean        majorProject;
    private String         technicalOffice;
    private String         status;
    private LocalDate      startDate;
    private LocalDate      endDate;
    private BigDecimal     revenueBudget;
    private BigDecimal     costBudget;
    private BigDecimal     marginBudget;
    private BigDecimal     projectMargin;
    private Long           createdById;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private OffsetDateTime deletedAt;
}