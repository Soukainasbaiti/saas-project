package com.segula.saasgestion.controller;

import com.segula.saasgestion.dto.ProjectWipDto;
import com.segula.saasgestion.dto.WipMonthDocumentDto;
import com.segula.saasgestion.dto.WipTableRowDto;
import com.segula.saasgestion.service.WipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/projects/{projectId}/wip")
@RequiredArgsConstructor
public class WipController {

    private final WipService wipService;

    // ── NOUVELLE API — Tableau mensuel ────────────────────────────
    @GetMapping("/table")
    @PreAuthorize("hasAnyRole('PM','BUM','ADMIN')")
    public List<WipTableRowDto> getTable(@PathVariable Long projectId) {
        return wipService.getWipTable(projectId);
    }

    // ── NOUVELLE API — Upload BL signé ou Bon de commande ─────────
    @PostMapping("/months/{year}/{month}/upload")
    @PreAuthorize("hasAnyRole('PM','ADMIN')")
    public ResponseEntity<WipMonthDocumentDto> upload(
            @PathVariable Long projectId,
            @PathVariable int year,
            @PathVariable int month,
            @RequestParam("type") String documentType,
            @RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(wipService.uploadDocument(projectId, year, month, documentType, file));
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // ── NOUVELLE API — Confirmer le montant extrait ───────────────
    @PutMapping("/documents/{docId}/confirm")
    @PreAuthorize("hasAnyRole('PM','ADMIN')")
    public ResponseEntity<WipMonthDocumentDto> confirmAmount(
            @PathVariable Long projectId,
            @PathVariable Long docId,
            @RequestBody Map<String, BigDecimal> body) {
        BigDecimal amount = body.get("confirmedAmount");
        return ResponseEntity.ok(wipService.confirmAmount(docId, amount));
    }

    // ── NOUVELLE API — Télécharger un document ────────────────────
    @GetMapping("/documents/{docId}/download")
    @PreAuthorize("hasAnyRole('PM','BUM','ADMIN')")
    public ResponseEntity<byte[]> download(
            @PathVariable Long projectId,
            @PathVariable Long docId) {
        try {
            byte[] bytes = wipService.downloadDocument(docId);
            String fileName = wipService.getDocumentFileName(docId);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ── NOUVELLE API — Supprimer un document ─────────────────────
    @DeleteMapping("/documents/{docId}")
    @PreAuthorize("hasAnyRole('PM','ADMIN')")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable Long projectId,
            @PathVariable Long docId) {
        wipService.deleteDocument(docId);
        return ResponseEntity.noContent().build();
    }

    // ── ANCIENNE API — liste des entrées WIP (conservée) ─────────
    @GetMapping
    @PreAuthorize("hasAnyRole('PM','BUM','ADMIN')")
    public List<ProjectWipDto> list(@PathVariable Long projectId) {
        return wipService.getByProject(projectId);
    }

    @PostMapping("/parse")
    @PreAuthorize("hasAnyRole('PM','ADMIN')")
    public ResponseEntity<List<ProjectWipDto>> parseBl(
            @PathVariable Long projectId,
            @RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(wipService.parseBl(projectId, file));
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/save-all")
    @PreAuthorize("hasAnyRole('PM','ADMIN')")
    public ResponseEntity<List<ProjectWipDto>> saveAll(
            @PathVariable Long projectId,
            @RequestBody List<ProjectWipDto> dtos) {
        dtos.forEach(d -> d.setProjectId(projectId));
        return ResponseEntity.ok(wipService.saveAll(projectId, dtos));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PM','ADMIN')")
    public ResponseEntity<ProjectWipDto> create(
            @PathVariable Long projectId,
            @RequestBody ProjectWipDto dto) {
        dto.setProjectId(projectId);
        return ResponseEntity.ok(wipService.save(projectId, dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('PM','BUM','ADMIN')")
    public ResponseEntity<ProjectWipDto> update(
            @PathVariable Long projectId,
            @PathVariable Long id,
            @RequestBody ProjectWipDto dto) {
        return ResponseEntity.ok(wipService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('PM','ADMIN')")
    public ResponseEntity<Void> delete(
            @PathVariable Long projectId,
            @PathVariable Long id) {
        wipService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
