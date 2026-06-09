package com.segula.saasgestion.service;

import com.segula.saasgestion.domain.ProjectOpportunity;
import com.segula.saasgestion.dto.CreateOpportunityRequest;
import com.segula.saasgestion.dto.ProjectOpportunityDto;
import com.segula.saasgestion.repository.ProjectOpportunityRepository;
import com.segula.saasgestion.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OpportunityService {

    private final ProjectOpportunityRepository opportunityRepo;
    private final ProjectRepository projectRepository;

    public List<ProjectOpportunityDto> getOpportunities(Long projectId) {
        return opportunityRepo.findByProjectId(projectId)
            .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public ProjectOpportunityDto createOpportunity(CreateOpportunityRequest req) {
        int count = opportunityRepo.countByProjectId(req.getProjectId());
        int year  = LocalDate.now().getYear();

        String projCode = "";
        var proj = projectRepository.findById(req.getProjectId());
        if (proj.isPresent() && proj.get().getProjectId() != null) {
            projCode = proj.get().getProjectId();
        }

        String oId = projCode.isEmpty()
            ? String.format("O__%04d_%04d", year, count + 1)
            : String.format("O_%s_%04d_%04d", projCode, year, count + 1);

        BigDecimal benefit = computeBenefit(req.getPrice(), req.getCosts());
        boolean overdue   = computeOverdue(req.getDeadline(), req.getStatus());

        ProjectOpportunity opp = ProjectOpportunity.builder()
            .projectId(req.getProjectId())
            .oId(oId)
            .identificationDate(req.getIdentificationDate())
            .opportunityDescription(req.getOpportunityDescription())
            .category(req.getCategory())
            .costs(req.getCosts())
            .price(req.getPrice())
            .estimatedBenefit(benefit)
            .percentNewPm(req.getPercentNewPm())
            .actionToBeTaken(req.getActionToBeTaken())
            .owner(req.getOwner())
            .deadline(req.getDeadline())
            .overdue(overdue)
            .onHold(req.getOnHold() != null ? req.getOnHold() : false)
            .status(req.getStatus() != null ? req.getStatus() : "Open")
            .copilValidation(req.getCopilValidation() != null ? req.getCopilValidation() : "Pending")
            .comments(req.getComments())
            .build();

        return toDto(opportunityRepo.save(opp));
    }

    @Transactional
    public ProjectOpportunityDto updateOpportunity(Long id, CreateOpportunityRequest req) {
        ProjectOpportunity opp = opportunityRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Opportunity not found: " + id));

        if (req.getIdentificationDate()    != null) opp.setIdentificationDate(req.getIdentificationDate());
        if (req.getOpportunityDescription()!= null) opp.setOpportunityDescription(req.getOpportunityDescription());
        if (req.getCategory()              != null) opp.setCategory(req.getCategory());
        if (req.getCosts()                 != null) opp.setCosts(req.getCosts());
        if (req.getPrice()                 != null) opp.setPrice(req.getPrice());
        if (req.getPercentNewPm()          != null) opp.setPercentNewPm(req.getPercentNewPm());
        if (req.getActionToBeTaken()       != null) opp.setActionToBeTaken(req.getActionToBeTaken());
        if (req.getOwner()                 != null) opp.setOwner(req.getOwner());
        if (req.getDeadline()              != null) opp.setDeadline(req.getDeadline());
        if (req.getOnHold()                != null) opp.setOnHold(req.getOnHold());
        if (req.getStatus()                != null) opp.setStatus(req.getStatus());
        if (req.getCopilValidation()       != null) opp.setCopilValidation(req.getCopilValidation());
        if (req.getComments()              != null) opp.setComments(req.getComments());

        opp.setEstimatedBenefit(computeBenefit(opp.getPrice(), opp.getCosts()));
        opp.setOverdue(computeOverdue(opp.getDeadline(), opp.getStatus()));

        return toDto(opportunityRepo.save(opp));
    }

    @Transactional
    public void deleteOpportunity(Long id) {
        opportunityRepo.deleteById(id);
    }

    // ── Helpers ───────────────────────────────────────────────────
    private BigDecimal computeBenefit(BigDecimal price, BigDecimal costs) {
        if (price == null) return null;
        if (costs == null) return price;
        return price.subtract(costs);
    }

    private boolean computeOverdue(LocalDate deadline, String status) {
        if (deadline == null) return false;
        boolean terminal = "Won".equals(status) || "Lost".equals(status);
        return !terminal && LocalDate.now().isAfter(deadline);
    }

    private ProjectOpportunityDto toDto(ProjectOpportunity o) {
        return ProjectOpportunityDto.builder()
            .id(o.getId()).projectId(o.getProjectId()).oId(o.getOId())
            .identificationDate(o.getIdentificationDate())
            .opportunityDescription(o.getOpportunityDescription())
            .category(o.getCategory()).costs(o.getCosts()).price(o.getPrice())
            .estimatedBenefit(o.getEstimatedBenefit())
            .percentNewPm(o.getPercentNewPm())
            .actionToBeTaken(o.getActionToBeTaken()).owner(o.getOwner())
            .deadline(o.getDeadline()).overdue(o.getOverdue())
            .onHold(o.getOnHold()).status(o.getStatus())
            .copilValidation(o.getCopilValidation()).comments(o.getComments())
            .build();
    }
}
