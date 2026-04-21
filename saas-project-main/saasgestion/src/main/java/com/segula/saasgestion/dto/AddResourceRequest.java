package com.segula.saasgestion.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor
public class AddResourceRequest {
    private Long projectId;
    private String matricule;
    private String personName;
    private String contractType;
}
