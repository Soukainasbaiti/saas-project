package com.segula.saasgestion.dto;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginResponse {
    private String  accessToken;
    private String  refreshToken;
    private boolean forceChange;
    private String  role;
    private String  fullName;
}