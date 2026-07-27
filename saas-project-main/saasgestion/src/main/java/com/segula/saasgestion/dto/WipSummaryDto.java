package com.segula.saasgestion.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class WipSummaryDto {
    private BigDecimal totalDeclared;
    private BigDecimal totalInvoiced;
    // Montant du dernier Bon de Commande confirmé (le plus récent remplace les précédents,
    // ex: avenant qui redonne le nouveau total autorisé). Null si aucun BC confirmé.
    private BigDecimal totalOrderAmount;
    private String orderPeriod; // YYYY-MM du BC de référence
    // totalOrderAmount - totalInvoiced. Null si totalOrderAmount est null.
    private BigDecimal remainingOnOrder;
}
