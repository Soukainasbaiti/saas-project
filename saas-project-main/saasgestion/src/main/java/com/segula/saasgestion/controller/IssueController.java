package com.segula.saasgestion.controller;

import com.segula.saasgestion.dto.CreateIssueRequest;
import com.segula.saasgestion.dto.IssueDocumentDto;
import com.segula.saasgestion.dto.ProjectIssueDto;
import com.segula.saasgestion.service.IssueService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
@Tag(name = "Registre Issues", description = "Gestion des issues projet (blocages, actions correctives)")
@SecurityRequirement(name = "bearerAuth")
public class IssueController {

    private final IssueService issueService;

    @GetMapping("/{projectId}/issues")
    public ResponseEntity<List<ProjectIssueDto>> getIssues(@PathVariable Long projectId) {
        return ResponseEntity.ok(issueService.getIssues(projectId));
    }

    @PostMapping("/{projectId}/issues")
    public ResponseEntity<ProjectIssueDto> createIssue(
            @PathVariable Long projectId,
            @RequestBody CreateIssueRequest req) {
        req.setProjectId(projectId);
        return ResponseEntity.ok(issueService.createIssue(req));
    }

    @PutMapping("/{projectId}/issues/{issueId}")
    public ResponseEntity<ProjectIssueDto> updateIssue(
            @PathVariable Long projectId,
            @PathVariable Long issueId,
            @RequestBody CreateIssueRequest req) {
        return ResponseEntity.ok(issueService.updateIssue(issueId, req));
    }

    @DeleteMapping("/{projectId}/issues/{issueId}")
    public ResponseEntity<Void> deleteIssue(
            @PathVariable Long projectId,
            @PathVariable Long issueId) {
        issueService.deleteIssue(issueId);
        return ResponseEntity.noContent().build();
    }

    // ── Pièces jointes (PDF) ──────────────────────────────────────
    @PostMapping("/{projectId}/issues/{issueId}/documents")
    public ResponseEntity<IssueDocumentDto> uploadDocument(
            @PathVariable Long projectId,
            @PathVariable Long issueId,
            @RequestParam("file") MultipartFile file) {
        try {
            return ResponseEntity.ok(issueService.uploadDocument(issueId, file));
        } catch (IOException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/{projectId}/issues/{issueId}/documents/{docId}/download")
    public ResponseEntity<byte[]> downloadDocument(
            @PathVariable Long projectId,
            @PathVariable Long issueId,
            @PathVariable Long docId) {
        try {
            byte[] bytes = issueService.downloadDocument(docId);
            String fileName = issueService.getDocumentFileName(docId);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(bytes);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{projectId}/issues/{issueId}/documents/{docId}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable Long projectId,
            @PathVariable Long issueId,
            @PathVariable Long docId) {
        issueService.deleteDocument(docId);
        return ResponseEntity.noContent().build();
    }
}
