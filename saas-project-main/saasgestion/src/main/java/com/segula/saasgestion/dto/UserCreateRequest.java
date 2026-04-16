package com.segula.saasgestion.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class UserCreateRequest {
    @NotBlank private String fullName;
    @NotBlank @Email private String email;
    @NotBlank @Size(min = 6) private String password;
    @NotBlank private String role; // ADMIN, PM, BUM
}
