package com.segula.saasgestion.controller;

import com.segula.saasgestion.dto.CreateRiskRequest;
import com.segula.saasgestion.dto.ProjectRiskDto;
import com.segula.saasgestion.service.RiskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class RiskController {

    private final RiskService riskService;

    @GetMapping("/{projectId}/risks")
    public ResponseEntity<List<ProjectRiskDto>> getRisks(@PathVariable Long projectId) {
        return ResponseEntity.ok(riskService.getRisks(projectId));
    }

    @PostMapping("/{projectId}/risks")
    public ResponseEntity<ProjectRiskDto> createRisk(
            @PathVariable Long projectId,
            @RequestBody CreateRiskRequest req) {
        req.setProjectId(projectId);
        return ResponseEntity.ok(riskService.createRisk(req));
    }

    @PutMapping("/{projectId}/risks/{riskId}")
    public ResponseEntity<ProjectRiskDto> updateRisk(
            @PathVariable Long projectId,
            @PathVariable Long riskId,
            @RequestBody CreateRiskRequest req) {
        return ResponseEntity.ok(riskService.updateRisk(riskId, req));
    }

    @DeleteMapping("/{projectId}/risks/{riskId}")
    public ResponseEntity<Void> deleteRisk(
            @PathVariable Long projectId,
            @PathVariable Long riskId) {
        riskService.deleteRisk(riskId);
        return ResponseEntity.noContent().build();
    }
}
