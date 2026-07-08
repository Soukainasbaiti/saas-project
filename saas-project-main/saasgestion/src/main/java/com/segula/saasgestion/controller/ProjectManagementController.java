package com.segula.saasgestion.controller;

import com.segula.saasgestion.domain.AppUser;
import com.segula.saasgestion.dto.*;
import com.segula.saasgestion.repository.AppUserRepository;
import com.segula.saasgestion.service.ProjectManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
@Tag(name = "Project Management", description = "Ressources, saisies de temps, coûts, livrables, work types, WBS et One Pager")
@SecurityRequirement(name = "bearerAuth")
public class ProjectManagementController {

    private final ProjectManagementService managementService;
    private final AppUserRepository userRepo;

    private String resolveFullName(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        return userRepo.findById(userId).map(AppUser::getFullName).orElse("Unknown");
    }

    private Long userId(Authentication auth) {
        return (Long) auth.getPrincipal();
    }

    @GetMapping("/{id}/management")
    public ResponseEntity<ProjectManagementDto> getManagement(@PathVariable Long id) {
        return ResponseEntity.ok(managementService.getProjectManagement(id));
    }

    @PostMapping("/management/resource")
    public ResponseEntity<?> addResource(@RequestBody AddResourceRequest req, Authentication auth) {
        try {
            managementService.addResource(req, userId(auth));
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/management/resource/{resourceId}")
    public ResponseEntity<?> deleteResource(@PathVariable Long resourceId, Authentication auth) {
        try {
            managementService.deleteResource(resourceId, userId(auth));
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/management/resource/{resourceId}/country")
    public ResponseEntity<?> updateResourceCountry(@PathVariable Long resourceId, @RequestBody Map<String, Long> req, Authentication auth) {
        try {
            managementService.updateResourceCountry(resourceId, req.get("countryId"), userId(auth));
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/management/entry")
    public ResponseEntity<?> saveEntry(@RequestBody SaveResourceEntryRequest req, Authentication auth) {
        try {
            managementService.saveResourceEntry(req, userId(auth));
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
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

    // ── Statut Réel/Prévision par mois (Revenus — Moyens / Synthèse Financière) ──
    @PutMapping("/{id}/management/month-status")
    public ResponseEntity<?> updateMonthStatus(@PathVariable Long id, @RequestBody List<MonthStatusDto> updates) {
        try {
            return ResponseEntity.ok(managementService.updateMonthStatus(id, updates));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(422).body(Map.of("error", e.getMessage()));
        }
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

    @PutMapping("/{id}/management/onepager-extras")
    public ResponseEntity<?> updateOnePagerExtras(@PathVariable Long id, @RequestBody ProjectManagementDto req) {
        try {
            managementService.updateOnePagerExtras(id, req);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Validation workflow ────────────────────────────────────────
    @PostMapping("/{id}/management/submit")
    public ResponseEntity<?> submit(@PathVariable Long id, Authentication auth) {
        try {
            managementService.submitForValidation(id, resolveFullName(auth));
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/management/validate")
    public ResponseEntity<?> validate(@PathVariable Long id, Authentication auth) {
        try {
            managementService.validateProject(id, resolveFullName(auth));
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/management/reject")
    public ResponseEntity<?> reject(@PathVariable Long id,
                                    @RequestBody Map<String, String> req,
                                    Authentication auth) {
        try {
            managementService.rejectProject(id, resolveFullName(auth), req.get("comment"));
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/management/bum/pending")
    public ResponseEntity<List<Map<String, Object>>> bumPending(Authentication auth) {
        return ResponseEntity.ok(managementService.getPendingForBum(resolveFullName(auth)));
    }

    @GetMapping("/management/pm/rejected")
    public ResponseEntity<List<Map<String, Object>>> pmRejected(Authentication auth) {
        Long userId = Long.parseLong(auth.getName());
        return ResponseEntity.ok(managementService.getRejectedForPm(userId));
    }

    @GetMapping("/management/bum/history")
    public ResponseEntity<List<Map<String, Object>>> bumHistory(Authentication auth) {
        String bumName = resolveFullName(auth);
        return ResponseEntity.ok(managementService.getValidationHistoryForBum(bumName));
    }

    // ── SDH Import ─────────────────────────────────────────────────
    @PostMapping("/{id}/management/import-sdh")
    public ResponseEntity<?> importSdh(@PathVariable Long id,
                                        @RequestParam("file") MultipartFile file,
                                        Authentication auth) {
        try {
            return ResponseEntity.ok(managementService.importSdhFile(id, file, userId(auth)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ── Changement type d'engagement ────────────────────────────────────
    @PutMapping("/{id}/engagement")
    public ResponseEntity<?> changeEngagement(
            @PathVariable Long id,
            @RequestBody java.util.Map<String, String> body) {
        try {
            return ResponseEntity.ok(managementService.changeEngagement(id, body.get("engagementType")));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    // ── Work Package — Deliverables ───────────────────────────────────

    @GetMapping("/{id}/deliverables")
    public ResponseEntity<List<ProjectDeliverableDto>> getDeliverables(@PathVariable Long id) {
        return ResponseEntity.ok(managementService.getDeliverables(id));
    }

    @PostMapping("/{id}/deliverables")
    public ResponseEntity<ProjectDeliverableDto> createDeliverable(
            @PathVariable Long id,
            @RequestBody CreateDeliverableRequest req) {
        req.setProjectId(id);
        return ResponseEntity.ok(managementService.createDeliverable(req));
    }

    @PutMapping("/{id}/deliverables/{delivId}")
    public ResponseEntity<ProjectDeliverableDto> updateDeliverable(
            @PathVariable Long id,
            @PathVariable Long delivId,
            @RequestBody UpdateDeliverableRequest req) {
        return ResponseEntity.ok(managementService.updateDeliverable(delivId, req));
    }

    @DeleteMapping("/{id}/deliverables/{delivId}")
    public ResponseEntity<Void> deleteDeliverable(
            @PathVariable Long id,
            @PathVariable Long delivId) {
        managementService.deleteDeliverable(delivId);
        return ResponseEntity.noContent().build();
    }

    // ── Unit of Work — Work Types ─────────────────────────────────────

    @GetMapping("/{id}/work-types")
    public ResponseEntity<List<ProjectWorkTypeDto>> getWorkTypes(@PathVariable Long id) {
        return ResponseEntity.ok(managementService.getWorkTypes(id));
    }

    @PostMapping("/{id}/work-types")
    public ResponseEntity<ProjectWorkTypeDto> createWorkType(
            @PathVariable Long id,
            @RequestBody CreateWorkTypeRequest req) {
        req.setProjectId(id);
        return ResponseEntity.ok(managementService.createWorkType(req));
    }

    @DeleteMapping("/{id}/work-types/{typeId}")
    public ResponseEntity<Void> deleteWorkType(
            @PathVariable Long id,
            @PathVariable Long typeId) {
        managementService.deleteWorkType(typeId);
        return ResponseEntity.noContent().build();
    }

    // ── Unit of Work — Tickets ────────────────────────────────────────

    @GetMapping("/{id}/work-tickets")
    public ResponseEntity<List<ProjectWorkTicketDto>> getWorkTickets(@PathVariable Long id) {
        return ResponseEntity.ok(managementService.getWorkTickets(id));
    }

    @PostMapping("/{id}/work-tickets")
    public ResponseEntity<ProjectWorkTicketDto> createWorkTicket(
            @PathVariable Long id,
            @RequestBody CreateWorkTicketRequest req) {
        req.setProjectId(id);
        return ResponseEntity.ok(managementService.createWorkTicket(req));
    }

    @PutMapping("/{id}/work-tickets/{ticketId}")
    public ResponseEntity<ProjectWorkTicketDto> updateWorkTicket(
            @PathVariable Long id,
            @PathVariable Long ticketId,
            @RequestBody UpdateWorkTicketRequest req) {
        return ResponseEntity.ok(managementService.updateWorkTicket(ticketId, req));
    }

    @DeleteMapping("/{id}/work-tickets/{ticketId}")
    public ResponseEntity<Void> deleteWorkTicket(
            @PathVariable Long id,
            @PathVariable Long ticketId) {
        managementService.deleteWorkTicket(ticketId);
        return ResponseEntity.noContent().build();
    }
}
