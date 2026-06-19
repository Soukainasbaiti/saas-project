package com.segula.saasgestion.controller;

import com.segula.saasgestion.dto.ReferenceDto;
import com.segula.saasgestion.repository.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ref")
@RequiredArgsConstructor
@Tag(name = "Référentiels", description = "Listes de référence accessibles aux PMs : BU, clients, industries, disciplines, engagements")
@SecurityRequirement(name = "bearerAuth")
public class ReferenceDataController {

    private final BuRepository                    buRepo;
    private final CustomerRepository              customerRepo;
    private final IndustryRepository              industryRepo;
    private final EngineeringDisciplineRepository disciplineRepo;
    private final EngagementRepository            engagementRepo;
    private final ProjectFunctionRepository       functionRepo;
    private final FrontFinancierRepository        frontFinancierRepo;
    private final AppUserRepository               userRepo;

    @GetMapping("/bus")
    public ResponseEntity<List<ReferenceDto>> bus() {
        return ResponseEntity.ok(buRepo.findAllByIsActiveTrueOrderByNameAsc().stream()
            .map(b -> ReferenceDto.builder().id(b.getId()).label(b.getName()).code(b.getTrigram()).build()).toList());
    }

    @GetMapping("/customers")
    public ResponseEntity<List<ReferenceDto>> customers() {
        return ResponseEntity.ok(customerRepo.findAllByIsActiveTrueOrderByNameAsc().stream()
            .map(c -> ReferenceDto.builder().id(c.getId()).label(c.getName()).code(c.getTrigram()).build()).toList());
    }

    @GetMapping("/industries")
    public ResponseEntity<List<ReferenceDto>> industries() {
        return ResponseEntity.ok(industryRepo.findAllByIsActiveTrueOrderByNameAsc().stream()
            .map(i -> ReferenceDto.builder().id(i.getId()).label(i.getName()).code(i.getTrigram()).build()).toList());
    }

    @GetMapping("/disciplines")
    public ResponseEntity<List<ReferenceDto>> disciplines() {
        return ResponseEntity.ok(disciplineRepo.findAllByIsActiveTrueOrderByNameAsc().stream()
            .map(d -> ReferenceDto.builder().id(d.getId()).label(d.getName()).build()).toList());
    }

    @GetMapping("/engagements")
    public ResponseEntity<List<ReferenceDto>> engagements() {
        return ResponseEntity.ok(engagementRepo.findAllByIsActiveTrueOrderByNameAsc().stream()
            .map(e -> ReferenceDto.builder().id(e.getId()).label(e.getName()).code(e.getEngagementType()).build()).toList());
    }

    @GetMapping("/functions")
    public ResponseEntity<List<ReferenceDto>> functions(
            @RequestParam(required = false) Long disciplineId) {
        var list = disciplineId != null
            ? functionRepo.findByDisciplineOrGlobal(disciplineId)
            : functionRepo.findAllByIsActiveTrueOrderByNameAsc();
        return ResponseEntity.ok(list.stream()
            .map(f -> ReferenceDto.builder().id(f.getId()).label(f.getName()).build()).toList());
    }

    @GetMapping("/front-financiers")
    public ResponseEntity<List<ReferenceDto>> frontFinanciers() {
        return ResponseEntity.ok(frontFinancierRepo.findAllByIsActiveTrueOrderByCodeAsc().stream()
            .map(ff -> ReferenceDto.builder().id(ff.getId()).label(ff.getCode()).code(ff.getLabel()).build()).toList());
    }

    @GetMapping("/pms")
    public ResponseEntity<List<ReferenceDto>> pms() {
        return ResponseEntity.ok(userRepo.findAllActivePMs().stream()
            .map(u -> ReferenceDto.builder().id(u.getId()).label(u.getFullName()).code(u.getEmail()).build()).toList());
    }
}