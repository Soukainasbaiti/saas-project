package com.segula.saasgestion.controller;

import com.segula.saasgestion.dto.CreateMipRequest;
import com.segula.saasgestion.dto.ProjectMipDto;
import com.segula.saasgestion.service.MipService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
@Tag(name = "Registre MIP", description = "Points d'amélioration importants (Most Important Points)")
@SecurityRequirement(name = "bearerAuth")
public class MipController {

    private final MipService mipService;

    @GetMapping("/{projectId}/mips")
    public ResponseEntity<List<ProjectMipDto>> getMips(@PathVariable Long projectId) {
        return ResponseEntity.ok(mipService.getMips(projectId));
    }

    @PostMapping("/{projectId}/mips")
    public ResponseEntity<ProjectMipDto> createMip(
            @PathVariable Long projectId,
            @RequestBody CreateMipRequest req) {
        req.setProjectId(projectId);
        return ResponseEntity.ok(mipService.createMip(req));
    }

    @PutMapping("/{projectId}/mips/{mipId}")
    public ResponseEntity<ProjectMipDto> updateMip(
            @PathVariable Long projectId,
            @PathVariable Long mipId,
            @RequestBody CreateMipRequest req) {
        return ResponseEntity.ok(mipService.updateMip(mipId, req));
    }

    @DeleteMapping("/{projectId}/mips/{mipId}")
    public ResponseEntity<Void> deleteMip(
            @PathVariable Long projectId,
            @PathVariable Long mipId) {
        mipService.deleteMip(mipId);
        return ResponseEntity.noContent().build();
    }
}
