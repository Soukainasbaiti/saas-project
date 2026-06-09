package com.segula.saasgestion.controller;

import com.segula.saasgestion.dto.CreateIssueRequest;
import com.segula.saasgestion.dto.ProjectIssueDto;
import com.segula.saasgestion.service.IssueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
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
}
