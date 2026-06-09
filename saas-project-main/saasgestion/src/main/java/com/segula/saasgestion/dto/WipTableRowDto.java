package com.segula.saasgestion.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class WipTableRowDto {
    private String period;          // YYYY-MM
    private BigDecimal declaredAmount;  // SUM(billed_days * daily_rate)
    private BigDecimal invoicedAmount;  // confirmed_amount du BL signé
    private BigDecimal delta;           // declared - invoiced
    private int docCount;
    private List<WipMonthDocumentDto> documents;
}
