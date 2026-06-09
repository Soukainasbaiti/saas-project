package com.segula.saasgestion.service;

import com.segula.saasgestion.domain.ProjectIssue;
import com.segula.saasgestion.dto.CreateIssueRequest;
import com.segula.saasgestion.dto.ProjectIssueDto;
import com.segula.saasgestion.repository.ProjectIssueRepository;
import com.segula.saasgestion.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IssueService {

    private final ProjectIssueRepository issueRepo;
    private final ProjectRepository projectRepository;

    // ── Impacts matrix : Severity x Priority ─────────────────────
    private static final Map<String, Map<String, String>> IMPACTS_MATRIX = Map.of(
        "High",   Map.of("P1-High","Critical Impact","P2-Medium","High Impact","P3-Low","Medium Impact"),
        "Medium", Map.of("P1-High","High Impact","P2-Medium","Medium Impact","P3-Low","Low Impact"),
        "Low",    Map.of("P1-High","Medium Impact","P2-Medium","Low Impact","P3-Low","Negligible Impact")
    );

    private String computeImpacts(String severity, String priority) {
        if (severity == null || priority == null) return null;
        Map<String, String> row = IMPACTS_MATRIX.get(severity);
        return row != null ? row.get(priority) : null;
    }

    private Integer computeResolutionTime(LocalDate dta, LocalDate dtr) {
        if (dta == null || dtr == null) return null;
        return (int) ChronoUnit.DAYS.between(dta, dtr);
    }

    // ── CRUD ──────────────────────────────────────────────────────
    public List<ProjectIssueDto> getIssues(Long projectId) {
        return issueRepo.findByProjectId(projectId)
            .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public ProjectIssueDto createIssue(CreateIssueRequest req) {
        int count = issueRepo.countByProjectId(req.getProjectId());
        int year  = LocalDate.now().getYear();

        // Récupérer le projectId métier pour l'inclure dans l'I-ID
        String projCode = "";
        var proj = projectRepository.findById(req.getProjectId());
        if (proj.isPresent() && proj.get().getProjectId() != null) {
            projCode = proj.get().getProjectId();
        }

        String iId = projCode.isEmpty()
            ? String.format("I__%04d_%04d", year, count + 1)
            : String.format("I_%s_%04d_%04d", projCode, year, count + 1);

        String impacts = computeImpacts(req.getSeverity(), req.getPriority());
        Integer resolutionTime = computeResolutionTime(req.getDta(), req.getDtr());

        // Si DTR est renseigné → status = Closed automatiquement
        String status = req.getDtr() != null ? "Closed"
            : (req.getStatus() != null ? req.getStatus() : "Open");

        ProjectIssue issue = ProjectIssue.builder()
            .projectId(req.getProjectId())
            .iId(iId)
            .issue(req.getIssue())
            .severity(req.getSeverity())
            .priority(req.getPriority())
            .impacts(impacts)
            .dte(req.getDte())
            .dta(req.getDta())
            .lockdown(req.getLockdown())
            .investigation(req.getInvestigation())
            .sustainableResolution(req.getSustainableResolution())
            .exitCriteria(req.getExitCriteria())
            .owner(req.getOwner())
            .qualityLeader(req.getQualityLeader())
            .deadline(req.getDeadline())
            .communication(req.getCommunication())
            .escaladeLevel(req.getEscaladeLevel())
            .escaladeDate(req.getEscaladeDate())
            .escaladeDecision(req.getEscaladeDecision())
            .link(req.getLink())
            .status(status)
            .dtr(req.getDtr())
            .resolutionTime(resolutionTime)
            .remarks(req.getRemarks())
            .build();

        return toDto(issueRepo.save(issue));
    }

    @Transactional
    public ProjectIssueDto updateIssue(Long id, CreateIssueRequest req) {
        ProjectIssue issue = issueRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Issue not found: " + id));

        if (req.getIssue()               != null) issue.setIssue(req.getIssue());
        if (req.getSeverity()            != null) issue.setSeverity(req.getSeverity());
        if (req.getPriority()            != null) issue.setPriority(req.getPriority());
        issue.setImpacts(computeImpacts(issue.getSeverity(), issue.getPriority()));
        if (req.getDte()                 != null) issue.setDte(req.getDte());
        if (req.getDta()                 != null) issue.setDta(req.getDta());
        if (req.getLockdown()            != null) issue.setLockdown(req.getLockdown());
        if (req.getInvestigation()       != null) issue.setInvestigation(req.getInvestigation());
        if (req.getSustainableResolution()!=null) issue.setSustainableResolution(req.getSustainableResolution());
        if (req.getExitCriteria()        != null) issue.setExitCriteria(req.getExitCriteria());
        if (req.getOwner()               != null) issue.setOwner(req.getOwner());
        if (req.getQualityLeader()       != null) issue.setQualityLeader(req.getQualityLeader());
        if (req.getDeadline()            != null) issue.setDeadline(req.getDeadline());
        if (req.getCommunication()       != null) issue.setCommunication(req.getCommunication());
        if (req.getEscaladeLevel()       != null) issue.setEscaladeLevel(req.getEscaladeLevel());
        if (req.getEscaladeDate()        != null) issue.setEscaladeDate(req.getEscaladeDate());
        if (req.getEscaladeDecision()    != null) issue.setEscaladeDecision(req.getEscaladeDecision());
        if (req.getLink()                != null) issue.setLink(req.getLink());
        if (req.getRemarks()             != null) issue.setRemarks(req.getRemarks());

        // DTR → status Closed + calcul resolution time
        issue.setDtr(req.getDtr());
        if (req.getDtr() != null) {
            issue.setStatus("Closed");
            issue.setResolutionTime(computeResolutionTime(issue.getDta(), req.getDtr()));
        } else if (req.getStatus() != null) {
            issue.setStatus(req.getStatus());
        }

        return toDto(issueRepo.save(issue));
    }

    @Transactional
    public void deleteIssue(Long id) {
        issueRepo.deleteById(id);
    }

    private ProjectIssueDto toDto(ProjectIssue i) {
        return ProjectIssueDto.builder()
            .id(i.getId()).projectId(i.getProjectId()).iId(i.getIId())
            .issue(i.getIssue()).severity(i.getSeverity()).priority(i.getPriority())
            .impacts(i.getImpacts()).dte(i.getDte()).dta(i.getDta())
            .lockdown(i.getLockdown()).investigation(i.getInvestigation())
            .sustainableResolution(i.getSustainableResolution()).exitCriteria(i.getExitCriteria())
            .owner(i.getOwner()).qualityLeader(i.getQualityLeader())
            .deadline(i.getDeadline()).communication(i.getCommunication())
            .escaladeLevel(i.getEscaladeLevel()).escaladeDate(i.getEscaladeDate())
            .escaladeDecision(i.getEscaladeDecision()).link(i.getLink())
            .status(i.getStatus()).dtr(i.getDtr())
            .resolutionTime(i.getResolutionTime()).remarks(i.getRemarks())
            .build();
    }
}
