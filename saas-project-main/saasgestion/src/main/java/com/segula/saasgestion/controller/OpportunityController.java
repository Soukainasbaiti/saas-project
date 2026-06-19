package com.segula.saasgestion.controller;

import com.segula.saasgestion.dto.CreateOpportunityRequest;
import com.segula.saasgestion.dto.ProjectOpportunityDto;
import com.segula.saasgestion.service.OpportunityService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
@Tag(name = "Registre Opportunités", description = "Gestion des opportunités commerciales identifiées dans le projet")
@SecurityRequirement(name = "bearerAuth")
public class OpportunityController {

    private final OpportunityService opportunityService;

    @GetMapping("/{projectId}/opportunities")
    public ResponseEntity<List<ProjectOpportunityDto>> getOpportunities(@PathVariable Long projectId) {
        return ResponseEntity.ok(opportunityService.getOpportunities(projectId));
    }

    @PostMapping("/{projectId}/opportunities")
    public ResponseEntity<ProjectOpportunityDto> createOpportunity(
            @PathVariable Long projectId,
            @RequestBody CreateOpportunityRequest req) {
        req.setProjectId(projectId);
        return ResponseEntity.ok(opportunityService.createOpportunity(req));
    }

    @PutMapping("/{projectId}/opportunities/{oppId}")
    public ResponseEntity<ProjectOpportunityDto> updateOpportunity(
            @PathVariable Long projectId,
            @PathVariable Long oppId,
            @RequestBody CreateOpportunityRequest req) {
        return ResponseEntity.ok(opportunityService.updateOpportunity(oppId, req));
    }

    @DeleteMapping("/{projectId}/opportunities/{oppId}")
    public ResponseEntity<Void> deleteOpportunity(
            @PathVariable Long projectId,
            @PathVariable Long oppId) {
        opportunityService.deleteOpportunity(oppId);
        return ResponseEntity.noContent().build();
    }
}
