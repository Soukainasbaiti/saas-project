package com.segula.saasgestion.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank
    @Size(min = 8, max = 100, message = "Le mot de passe doit contenir au moins 8 caracteres")
    @Pattern(
        regexp = "^(?=.*[A-Z])(?=.*[0-9]).{8,}$",
        message = "Le mot de passe doit contenir au moins une majuscule et un chiffre"
    )
    private String newPassword;
}