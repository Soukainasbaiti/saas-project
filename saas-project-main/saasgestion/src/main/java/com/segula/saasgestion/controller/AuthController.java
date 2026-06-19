package com.segula.saasgestion.controller;

import com.segula.saasgestion.dto.ChangePasswordRequest;
import com.segula.saasgestion.dto.LoginRequest;
import com.segula.saasgestion.dto.LoginResponse;
import com.segula.saasgestion.dto.RefreshRequest;
import com.segula.saasgestion.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification", description = "Login, refresh token et changement de mot de passe")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Login", description = "Authentifie un utilisateur et retourne les tokens JWT access + refresh")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        try {
            return ResponseEntity.ok(authService.login(req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(403).build();
        }
    }

    @Operation(summary = "Changer le mot de passe", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest req,
            Authentication auth) {

        Long userId = (Long) auth.getPrincipal();
        authService.changePassword(userId, req);
        return ResponseEntity.ok(Map.of("message", "Mot de passe mis à jour avec succès"));
    }

    @Operation(summary = "Rafraîchir les tokens", description = "Génère un nouveau access token à partir du refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshRequest req) {
        try {
            return ResponseEntity.ok(authService.refresh(req.getRefreshToken()));
        } catch (Exception e) {
            return ResponseEntity.status(401).build();
        }
    }
}