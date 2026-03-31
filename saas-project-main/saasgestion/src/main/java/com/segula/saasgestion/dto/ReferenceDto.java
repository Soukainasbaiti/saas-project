package com.segula.saasgestion.dto;
import lombok.*;


// ─── ReferenceDto ─────────────────────────────────────────────────────────────
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ReferenceDto {
    private Object id;
    private String label;
    private String code;
 
}