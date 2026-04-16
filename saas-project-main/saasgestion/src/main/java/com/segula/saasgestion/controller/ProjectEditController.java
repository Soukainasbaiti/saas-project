package com.segula.saasgestion.controller;

import com.segula.saasgestion.dto.ProjectCreateRequest;
import com.segula.saasgestion.service.ProjectPendingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/projects/pending")
@RequiredArgsConstructor
public class ProjectEditController {

    private final ProjectPendingService pendingService;

    // GET /projects/pending/{token}/edit
    // Retourne le projet rejeté pour modification
    @GetMapping("/{token}/edit")
    public ResponseEntity<?> getForEdit(@PathVariable String token) {
        try {
            var detail = pendingService.getForEdit(token);
            return ResponseEntity.ok(Map.of(
                "token",       token,
                "status",      detail.pending().getStatus(),
                "submittedBy", detail.pending().getSubmittedBy().getFullName(),
                "project",     detail.request()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    // DELETE /projects/pending/{token}
    // Supprime définitivement un projet rejeté
    @DeleteMapping("/{token}")
    public ResponseEntity<?> delete(@PathVariable String token, Authentication auth) {
        try {
            Long userId = (Long) auth.getPrincipal();
            pendingService.deleteRejected(token, userId);
            return ResponseEntity.ok(Map.of("message", "Projet supprimé définitivement."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    // POST /projects/pending/{token}/resubmit
    // Resoumet le projet modifié
    @PostMapping("/{token}/resubmit")
    public ResponseEntity<?> resubmit(
            @PathVariable String token,
            @Valid @RequestBody ProjectCreateRequest req,
            Authentication auth) {
        try {
            Long submitterId = (Long) auth.getPrincipal();
            String newToken = pendingService.resubmit(token, req, submitterId);
            return ResponseEntity.accepted().body(Map.of(
                "message",      "Projet resoumis avec succès. En attente de validation.",
                "approvalToken", newToken
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }
}
