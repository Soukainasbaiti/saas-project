package com.segula.saasgestion.controller;

import com.segula.saasgestion.dto.*;
import com.segula.saasgestion.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<PagedResponse<ProjectListDto>> list(
            @RequestParam(required = false) String buId,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Short year,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(projectService.findAll(buId, customerId, status, year, search, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProjectDetailDto> detail(@PathVariable Long id) {
        ProjectDetailDto proj = projectService.findById(id);
        if (proj == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(proj);
    }

    @PostMapping
    public ResponseEntity<ProjectDetailDto> create(@Valid @RequestBody ProjectCreateRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> archive(@PathVariable Long id) {
        projectService.archive(id);
        return ResponseEntity.noContent().build();
    }

    // ✅ Endpoint stats dashboard ajouté
    @GetMapping("/stats/dashboard")
    public ResponseEntity<DashboardStatsDto> dashboardStats(
            @RequestParam(required = false) Short year) {
        return ResponseEntity.ok(projectService.getDashboardStats(year));
    }
}