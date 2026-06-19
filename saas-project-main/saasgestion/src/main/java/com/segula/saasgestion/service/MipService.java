package com.segula.saasgestion.service;

import com.segula.saasgestion.domain.ProjectMip;
import com.segula.saasgestion.dto.CreateMipRequest;
import com.segula.saasgestion.dto.ProjectMipDto;
import com.segula.saasgestion.repository.ProjectMipRepository;
import com.segula.saasgestion.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MipService {

    private final ProjectMipRepository mipRepo;
    private final ProjectRepository projectRepository;

    public List<ProjectMipDto> getMips(Long projectId) {
        return mipRepo.findByProjectId(projectId)
            .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public ProjectMipDto createMip(CreateMipRequest req) {
        int count = mipRepo.countByProjectId(req.getProjectId());
        int year  = LocalDate.now().getYear();

        String projCode = "";
        var proj = projectRepository.findById(req.getProjectId());
        if (proj.isPresent() && proj.get().getProjectId() != null) {
            projCode = proj.get().getProjectId();
        }

        String mipId = projCode.isEmpty()
            ? String.format("MIP__%04d_%04d", year, count + 1)
            : String.format("MIP_%s_%04d_%04d", projCode, year, count + 1);

        ProjectMip mip = ProjectMip.builder()
            .projectId(req.getProjectId())
            .mipId(mipId)
            .identificationDate(req.getIdentificationDate())
            .lever(req.getLever())
            .actionDescription(req.getActionDescription())
            .accountable(req.getAccountable())
            .clientImpact(req.getClientImpact())
            .dueDate(req.getDueDate())
            .priority(req.getPriority())
            .risquesPrerequis(req.getRisquesPrerequis())
            .plannedGain(req.getPlannedGain())
            .realizedGain(req.getRealizedGain())
            .status(req.getStatus() != null ? req.getStatus() : "Identified")
            .build();

        ProjectMipDto result = toDto(mipRepo.save(mip));
        log.info("MIP créé : {} projet={}", mipId, req.getProjectId());
        return result;
    }

    @Transactional
    public ProjectMipDto updateMip(Long id, CreateMipRequest req) {
        ProjectMip mip = mipRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("MIP not found: " + id));

        if (req.getIdentificationDate() != null) mip.setIdentificationDate(req.getIdentificationDate());
        if (req.getLever()              != null) mip.setLever(req.getLever());
        if (req.getActionDescription()  != null) mip.setActionDescription(req.getActionDescription());
        if (req.getAccountable()        != null) mip.setAccountable(req.getAccountable());
        if (req.getClientImpact()       != null) mip.setClientImpact(req.getClientImpact());
        if (req.getDueDate()            != null) mip.setDueDate(req.getDueDate());
        if (req.getPriority()           != null) mip.setPriority(req.getPriority());
        if (req.getRisquesPrerequis()   != null) mip.setRisquesPrerequis(req.getRisquesPrerequis());
        if (req.getPlannedGain()        != null) mip.setPlannedGain(req.getPlannedGain());
        if (req.getRealizedGain()       != null) mip.setRealizedGain(req.getRealizedGain());
        if (req.getStatus()             != null) mip.setStatus(req.getStatus());

        return toDto(mipRepo.save(mip));
    }

    @Transactional
    public void deleteMip(Long id) {
        mipRepo.deleteById(id);
        log.info("MIP supprimé : id={}", id);
    }

    private ProjectMipDto toDto(ProjectMip m) {
        return ProjectMipDto.builder()
            .id(m.getId()).projectId(m.getProjectId()).mipId(m.getMipId())
            .identificationDate(m.getIdentificationDate()).lever(m.getLever())
            .actionDescription(m.getActionDescription()).accountable(m.getAccountable())
            .clientImpact(m.getClientImpact()).dueDate(m.getDueDate()).priority(m.getPriority())
            .risquesPrerequis(m.getRisquesPrerequis()).plannedGain(m.getPlannedGain())
            .realizedGain(m.getRealizedGain()).status(m.getStatus())
            .build();
    }
}
