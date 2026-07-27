package com.segula.saasgestion.controller;

import com.segula.saasgestion.dto.*;
import com.segula.saasgestion.repository.AppUserRepository;
import com.segula.saasgestion.service.ProjectCountryService;
import com.segula.saasgestion.service.ProjectPendingService;
import com.segula.saasgestion.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
@Tag(name = "Projets", description = "CRUD projets, dashboard stats et prévisions mensuelles")
@SecurityRequirement(name = "bearerAuth")
public class ProjectController {

    private final ProjectService        projectService;
    private final ProjectPendingService pendingService;
    private final ProjectCountryService projectCountryService;
    private final AppUserRepository     userRepo;

    @Operation(summary = "Lister les projets", description = "Pagination + filtres BU, client, statut, année, recherche texte")
    @GetMapping
    public ResponseEntity<PagedResponse<ProjectListDto>> list(
            @RequestParam(required = false) String buId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Short year,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication auth) {

        Long userId = (Long) auth.getPrincipal();
        String role = userRepo.findById(userId)
                .map(u -> u.getRole()).orElse("PM");

        // ADMIN voit tous les projets, les autres seulement les leurs
        Long createdById = "ADMIN".equals(role) ? null : userId;

        return ResponseEntity.ok(
            projectService.findAll(buId, customerId, status, year, search, createdById, page, size)
        );
    }

    @Operation(summary = "Détail d'un projet")
    @GetMapping("/{id}")
    public ResponseEntity<ProjectDetailDto> detail(@PathVariable Long id) {
        ProjectDetailDto proj = projectService.findById(id);
        if (proj == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(proj);
    }

    @Operation(summary = "Créer un projet", description = "Soumet le projet pour approbation par l'admin (envoi email Brevo)")
    @PostMapping
    public ResponseEntity<Map<String, String>> create(
            @Valid @RequestBody ProjectCreateRequest req,
            Authentication auth) {

        Long submitterId = (Long) auth.getPrincipal();
        String token = pendingService.submitForApproval(req, submitterId);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(Map.of(
                    "message", "Projet soumis avec succes. En attente de validation par l'administrateur.",
                    "approvalToken", token
                ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody ProjectCreateRequest req) {
        try {
            return ResponseEntity.ok(projectService.update(id, req));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(422).body(Map.of("error", e.getMessage()));
        }
    }

    // ── PUT : mise a jour des previsions mensuelles (Revenue/Cost/COV) ──
    @PutMapping("/{id}/monthly-forecasts")
    public ResponseEntity<?> updateMonthlyForecasts(
            @PathVariable Long id,
            @RequestBody java.util.List<MonthlyForecastDto> updates) {
        try {
            return ResponseEntity.ok(projectService.updateMonthlyForecasts(id, updates));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(422).body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archive(@PathVariable Long id) {
        projectService.archive(id);
        return ResponseEntity.noContent().build();
    }

    // ── Pays du projet (multi-pays) ──────────────────────────────────
    @Operation(summary = "Lister les pays rattachés à un projet")
    @GetMapping("/{id}/countries")
    public ResponseEntity<java.util.List<ProjectCountryDto>> listCountries(@PathVariable Long id) {
        return ResponseEntity.ok(projectCountryService.listByProject(id));
    }

    @Operation(summary = "Ajouter un pays à un projet existant",
            description = "Déclenchable par l'Admin ou le PM Front Office. Pas de validation bloquante : notifie le PM assigné et l'Admin par email.")
    @PostMapping("/{id}/countries")
    public ResponseEntity<?> addCountry(
            @PathVariable Long id,
            @RequestBody AddProjectCountryRequest req,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(projectCountryService.addCountry(id, req, userId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(422).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Assigner/changer le Chef de Projet d'un pays du projet")
    @PatchMapping("/{id}/countries/{countryId}/pm")
    public ResponseEntity<?> assignCountryPm(
            @PathVariable Long id,
            @PathVariable Long countryId,
            @RequestBody Map<String, Long> body) {
        try {
            return ResponseEntity.ok(projectCountryService.assignPm(id, countryId, body.get("pmId")));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(422).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Retirer un pays du projet",
            description = "Corrige une erreur de saisie. Impossible sur le pays chef de file ou si des données existent déjà pour ce pays.")
    @DeleteMapping("/{id}/countries/{countryId}")
    public ResponseEntity<?> deleteCountry(
            @PathVariable Long id,
            @PathVariable Long countryId,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        try {
            projectCountryService.deleteCountry(id, countryId, userId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(422).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Stats dashboard", description = "KPIs globaux : budget total, CA réalisé, taux de rentabilité, répartition statuts")
    @GetMapping("/stats/dashboard")
    public ResponseEntity<DashboardStatsDto> dashboardStats(
            @RequestParam(required = false) Short year,
            Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = userRepo.findById(userId).map(u -> u.getRole()).orElse("PM");
        Long createdById = "ADMIN".equals(role) ? null : userId;
        return ResponseEntity.ok(projectService.getDashboardStats(year, createdById));
    }
}