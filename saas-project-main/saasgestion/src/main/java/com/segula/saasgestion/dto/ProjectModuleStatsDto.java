package com.segula.saasgestion.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectModuleStatsDto {
    private Long projectId;
    private long riskCount;
    private long openIssueCount;
    private long openOpportunityCount;
    private long mipCount;
    private BigDecimal wipTotalAmount;
    private long wipEntryCount;
}
