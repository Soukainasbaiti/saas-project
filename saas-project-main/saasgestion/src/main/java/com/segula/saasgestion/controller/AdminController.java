package com.segula.saasgestion.controller;

import com.segula.saasgestion.domain.AppUser;
import com.segula.saasgestion.dto.*;
import com.segula.saasgestion.repository.AppUserRepository;
import com.segula.saasgestion.repository.ProjectRiskRepository;
import com.segula.saasgestion.repository.ProjectIssueRepository;
import com.segula.saasgestion.repository.ProjectOpportunityRepository;
import com.segula.saasgestion.repository.ProjectMipRepository;
import com.segula.saasgestion.repository.ProjectWipRepository;
import com.segula.saasgestion.repository.ProjectRepository;
import com.segula.saasgestion.service.ProjectPendingService;
import com.segula.saasgestion.service.ProjectService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Administration", description = "Gestion des utilisateurs, approbation des projets, tableau de bord admin")
@SecurityRequirement(name = "bearerAuth")
public class AdminController {

    private final ProjectPendingService     pendingService;
    private final ProjectService            projectService;
    private final AppUserRepository         userRepo;
    private final PasswordEncoder           passwordEncoder;
    private final ProjectRiskRepository     riskRepo;
    private final ProjectIssueRepository    issueRepo;
    private final ProjectOpportunityRepository oppRepo;
    private final ProjectMipRepository      mipRepo;
    private final ProjectWipRepository      wipRepo;
    private final ProjectRepository         projectRepo;

    // ── GET /admin/approve/{token} ────────────────────────────────
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

    // ── POST /admin/approve/{token} ───────────────────────────────
    @PostMapping("/approve/{token}")
    public ResponseEntity<?> processApproval(
            @PathVariable String token,
            @RequestBody Map<String, String> body,
            Authentication auth) {

        // auth peut être null si l'admin n'est pas connecté (accès via lien email)
        Long adminId = (auth != null && auth.getPrincipal() instanceof Long)
                ? (Long) auth.getPrincipal()
                : null;
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
            return ResponseEntity.status(422).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /admin/users ──────────────────────────────────────────
    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> listUsers() {
        List<AppUser> all = userRepo.findAll();
        List<Map<String, Object>> users = all.stream()
            .filter(u -> u.getDeletedAt() == null)
            .sorted(Comparator.comparing(AppUser::getFullName))
            .map(u -> {
                String createdByName = (u.getCreatedById() != null)
                    ? all.stream().filter(a -> a.getId().equals(u.getCreatedById()))
                         .findFirst().map(AppUser::getFullName).orElse("—")
                    : "—";
                java.util.Map<String, Object> m = new java.util.HashMap<>();
                m.put("id",            u.getId());
                m.put("fullName",      u.getFullName());
                m.put("email",         u.getEmail());
                m.put("role",          u.getRole());
                m.put("isActive",      u.isActive());
                m.put("createdAt",     u.getCreatedAt() != null ? u.getCreatedAt().toString() : "");
                m.put("createdByName", createdByName);
                return m;
            })
            .toList();
        return ResponseEntity.ok(users);
    }

    private static final java.util.List<String> ALLOWED_DOMAINS =
        java.util.List.of("@segulagrp.com", "@segula.com", "@segula.fr");

    // ── POST /admin/users ─────────────────────────────────────────
    @PostMapping("/users")
    public ResponseEntity<?> createUser(@Valid @RequestBody UserCreateRequest req, Authentication auth) {
        String email = req.getEmail().toLowerCase().trim();
        boolean domainOk = ALLOWED_DOMAINS.stream().anyMatch(email::endsWith);
        if (!domainOk) {
            return ResponseEntity.status(422).body(Map.of(
                "error", "L'email doit se terminer par @segulagrp.com, @segula.com ou @segula.fr"
            ));
        }
        if (userRepo.findByEmailIgnoreCaseAndDeletedAtIsNull(req.getEmail()).isPresent()) {
            return ResponseEntity.status(409).body(Map.of("error", "Cet email est déjà utilisé."));
        }
        Long creatorId = (auth != null && auth.getPrincipal() instanceof Long)
            ? (Long) auth.getPrincipal() : null;
        AppUser user = AppUser.builder()
            .fullName(req.getFullName().trim())
            .email(req.getEmail().toLowerCase().trim())
            .passwordHash(passwordEncoder.encode(req.getPassword()))
            .role(req.getRole().toUpperCase().trim())
            .isActive(true)
            .mustChangePassword(true)
            .createdById(creatorId)
            .build();
        userRepo.save(user);
        log.info("Nouvel utilisateur créé: {} ({}) par adminId={}", user.getEmail(), user.getRole(), creatorId);
        return ResponseEntity.status(201).body(Map.of("message", "Utilisateur créé avec succès."));
    }

    // ── PUT /admin/users/{id} ─────────────────────────────────────
    @PutMapping("/users/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id,
                                        @RequestBody Map<String, Object> body) {
        AppUser user = userRepo.findById(id).orElse(null);
        if (user == null || user.getDeletedAt() != null) {
            return ResponseEntity.notFound().build();
        }
        if (body.containsKey("fullName")) {
            user.setFullName(body.get("fullName").toString().trim());
        }
        if (body.containsKey("email")) {
            String newEmail = body.get("email").toString().toLowerCase().trim();
            boolean domainOk = ALLOWED_DOMAINS.stream().anyMatch(newEmail::endsWith);
            if (!domainOk) {
                return ResponseEntity.status(422).body(Map.of(
                    "error", "L'email doit se terminer par @segulagrp.com, @segula.com ou @segula.fr"
                ));
            }
            boolean taken = userRepo.findByEmailIgnoreCaseAndDeletedAtIsNull(newEmail)
                .map(u -> !u.getId().equals(id)).orElse(false);
            if (taken) {
                return ResponseEntity.status(409).body(Map.of("error", "Cet email est déjà utilisé."));
            }
            user.setEmail(newEmail);
        }
        if (body.containsKey("role")) {
            user.setRole(body.get("role").toString().toUpperCase().trim());
        }
        if (body.containsKey("isActive")) {
            user.setActive(Boolean.parseBoolean(body.get("isActive").toString()));
        }
        userRepo.save(user);
        log.info("Utilisateur modifié: id={}", id);
        return ResponseEntity.ok(Map.of("message", "Utilisateur modifié avec succès."));
    }

    // ── DELETE /admin/users/{id} ──────────────────────────────────
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        AppUser user = userRepo.findById(id).orElse(null);
        if (user == null || user.getDeletedAt() != null) {
            return ResponseEntity.notFound().build();
        }
        user.setDeletedAt(java.time.OffsetDateTime.now());
        user.setActive(false);
        userRepo.save(user);
        log.info("Utilisateur supprimé (soft): id={}", id);
        return ResponseEntity.ok(Map.of("message", "Utilisateur supprimé avec succès."));
    }

    // ── GET /admin/module-stats ───────────────────────────────────
    // Stats modules (Risk/Issue/Opportunity/MIP/WIP) pour tous les projets
    @GetMapping("/module-stats")
    public ResponseEntity<List<ProjectModuleStatsDto>> getModuleStats() {
        List<Long> projectIds = projectRepo.findAll().stream()
            .map(p -> p.getId()).toList();

        List<ProjectModuleStatsDto> stats = projectIds.stream().map(pid -> {
            BigDecimal wipAmt = wipRepo.sumOpenWipAmountByProjectId(pid);
            return new ProjectModuleStatsDto(
                pid,
                riskRepo.countByProjectId(pid),
                issueRepo.countByProjectId(pid),
                oppRepo.countByProjectId(pid),
                mipRepo.countByProjectId(pid),
                wipAmt != null ? wipAmt : BigDecimal.ZERO,
                wipRepo.countByProjectId(pid)
            );
        }).toList();

        return ResponseEntity.ok(stats);
    }

    // ── GET /admin/wip-global ─────────────────────────────────────
    // WIP total global (tous projets, status != CLOSED)
    @GetMapping("/wip-global")
    public ResponseEntity<Map<String, Object>> getWipGlobal() {
        BigDecimal total = wipRepo.sumAllOpenWipAmount();
        return ResponseEntity.ok(Map.of(
            "totalWipAmount", total != null ? total : BigDecimal.ZERO
        ));
    }

    // ── POST /admin/projects/direct ───────────────────────────────
    // Création directe par l'admin (sans approbation)
    @PostMapping("/projects/direct")
    public ResponseEntity<?> createProjectDirect(
            @Valid @RequestBody ProjectCreateRequest req,
            Authentication auth) {
        try {
            Long adminId = (Long) auth.getPrincipal();
            ProjectDetailDto created = projectService.createDirect(req, adminId);
            return ResponseEntity.status(201).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(422).body(Map.of("error", e.getMessage()));
        }
    }
}
