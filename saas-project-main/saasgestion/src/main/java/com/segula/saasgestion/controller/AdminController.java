package com.segula.saasgestion.controller;

import com.segula.saasgestion.dto.ProjectDetailDto;
import com.segula.saasgestion.service.ProjectPendingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/admin")
 @RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final ProjectPendingService pendingService;

    // GET /admin/approve/{token}
    // Retourne les détails du projet en attente (page admin Angular)
    @GetMapping("/approve/{token}")
    public ResponseEntity<?> getPendingDetail(@PathVariable String token) {
        try {
            var detail = pendingService.getPendingDetail(token);
            return ResponseEntity.ok(Map.of(
                "token",       token,
                "status",      detail.pending().getStatus(),
                "submittedBy", detail.pending().getSubmittedBy().getFullName(),
                "submittedAt", detail.pending().getCreatedAt(),
                "expiresAt",   detail.pending().getExpiresAt(),
                "project",     detail.request()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    // POST /admin/approve/{token}
    // body: { "action": "approve" | "reject", "reason": "..." }
    @PostMapping("/approve/{token}")
    public ResponseEntity<?> processApproval(
            @PathVariable String token,
            @RequestBody Map<String, String> body,
            Authentication auth) {

        Long   adminId = (Long) auth.getPrincipal();
        String action  = body.getOrDefault("action", "");
        String reason  = body.get("reason");

        try {
            if ("approve".equalsIgnoreCase(action)) {
                ProjectDetailDto created = pendingService.approve(token, adminId);
                return ResponseEntity.ok(Map.of(
                    "message",   "Projet approuvé et créé avec succès",
                    "projectId", created.getId()
                ));
            } else if ("reject".equalsIgnoreCase(action)) {
                pendingService.reject(token, adminId, reason);
                return ResponseEntity.ok(Map.of("message", "Projet rejeté"));
            } else {
                return ResponseEntity.badRequest()
                    .body(Map.of("error", "action doit être 'approve' ou 'reject'"));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }
}