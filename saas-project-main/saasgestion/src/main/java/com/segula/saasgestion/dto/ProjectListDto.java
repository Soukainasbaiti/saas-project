package com.segula.saasgestion.dto;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ProjectListDto {
    private Long       id;
    private String     projectCode;
    private String     projectId; // ✅ ajouté
    private String     projectName;
    private Short      projectYear;
    private String     frontFinancier;
    private String     buId;
    private String     buName;
    private String     buTrigram;
    private String     customerName;
    private String     customerTrigram;
    private String     industryTrigram;
    private String     projectManager;
    private String     engagementType;
    private String     technicalOffice;
    private String     activity;
    private String     status;
    private boolean    majorProject;
    private LocalDate  startDate;
    private LocalDate  endDate;
    private BigDecimal revenueBudget;
    private BigDecimal costBudget;
    private BigDecimal marginBudget;
    private BigDecimal projectMargin;
}