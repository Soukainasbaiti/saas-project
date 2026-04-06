package com.segula.saasgestion.controller;

import com.segula.saasgestion.dto.*;
import com.segula.saasgestion.service.ProjectPendingService;
import com.segula.saasgestion.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService        projectService;
    private final ProjectPendingService pendingService;

    @GetMapping
    public ResponseEntity<PagedResponse<ProjectListDto>> list(
            @RequestParam(required = false) String buId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Short year,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
            projectService.findAll(buId, customerId, status, year, search, page, size)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectDetailDto> detail(@PathVariable Long id) {
        ProjectDetailDto proj = projectService.findById(id);
        if (proj == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(proj);
    }

    // ── POST : soumet pour approbation admin ──────────────────────
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archive(@PathVariable Long id) {
        projectService.archive(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats/dashboard")
    public ResponseEntity<DashboardStatsDto> dashboardStats(
            @RequestParam(required = false) Short year) {
        return ResponseEntity.ok(projectService.getDashboardStats(year));
    }
}