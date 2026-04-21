package com.segula.saasgestion.controller;

import com.segula.saasgestion.dto.*;
import com.segula.saasgestion.service.ProjectManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectManagementController {

    private final ProjectManagementService managementService;

    @GetMapping("/{id}/management")
    public ResponseEntity<ProjectManagementDto> getManagement(@PathVariable Long id) {
        return ResponseEntity.ok(managementService.getProjectManagement(id));
    }

    @PostMapping("/management/resource")
    public ResponseEntity<Void> addResource(@RequestBody AddResourceRequest req) {
        managementService.addResource(req);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/management/resource/{resourceId}")
    public ResponseEntity<Void> deleteResource(@PathVariable Long resourceId) {
        managementService.deleteResource(resourceId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/management/resource/{resourceId}/contract")
    public ResponseEntity<Void> updateContractType(@PathVariable Long resourceId, @RequestBody Map<String, String> req) {
        managementService.updateResourceContractType(resourceId, req.get("contractType"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/management/entry")
    public ResponseEntity<Void> saveEntry(@RequestBody SaveResourceEntryRequest req) {
        managementService.saveResourceEntry(req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/management/cost")
    public ResponseEntity<Void> saveCost(@RequestBody SaveOtherCostRequest req) {
        managementService.saveOtherCost(req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/management/cost/category")
    public ResponseEntity<Void> addCategory(@RequestBody Map<String, Object> req) {
        Long projectId = Long.valueOf(req.get("projectId").toString());
        String category = req.get("category").toString();
        managementService.addCategory(projectId, category);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/management/cost/category")
    public ResponseEntity<Void> deleteCategory(@RequestBody Map<String, Object> req) {
        Long projectId = Long.valueOf(req.get("projectId").toString());
        String category = req.get("category").toString();
        managementService.deleteCategory(projectId, category);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/management/cost/rebill")
    public ResponseEntity<Void> setCategoryRebill(@RequestBody Map<String, Object> req) {
        Long projectId = Long.valueOf(req.get("projectId").toString());
        String category = req.get("category").toString();
        boolean isRebill = Boolean.parseBoolean(req.get("isRebill").toString());
        managementService.setCategoryRebill(projectId, category, isRebill);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/management/granularity")
    public ResponseEntity<?> setGranularity(@PathVariable Long id, @RequestBody Map<String, String> req) {
        try {
            managementService.setGranularity(id, req.get("granularity"), req.get("currency"));
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
