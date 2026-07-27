package com.segula.saasgestion.service;

import com.segula.saasgestion.domain.IssueDocument;
import com.segula.saasgestion.domain.ProjectIssue;
import com.segula.saasgestion.dto.CreateIssueRequest;
import com.segula.saasgestion.dto.IssueDocumentDto;
import com.segula.saasgestion.dto.ProjectIssueDto;
import com.segula.saasgestion.repository.IssueDocumentRepository;
import com.segula.saasgestion.repository.ProjectIssueRepository;
import com.segula.saasgestion.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IssueService {

    private final ProjectIssueRepository issueRepo;
    private final ProjectRepository projectRepository;
    private final IssueDocumentRepository docRepo;

    private static final String UPLOAD_DIR = "./uploads/issues/";

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
        List<ProjectIssue> issues = issueRepo.findByProjectId(projectId);
        List<Long> ids = issues.stream().map(ProjectIssue::getId).collect(Collectors.toList());
        Map<Long, List<IssueDocument>> docsByIssue = docRepo.findByIssueIdIn(ids).stream()
            .collect(Collectors.groupingBy(IssueDocument::getIssueId));
        return issues.stream()
            .map(i -> toDto(i, docsByIssue.getOrDefault(i.getId(), List.of()).stream()
                .map(this::toDocDto).collect(Collectors.toList())))
            .collect(Collectors.toList());
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

        ProjectIssueDto result = toDto(issueRepo.save(issue));
        log.info("Issue créée : {} projet={} sévérité={}", iId, req.getProjectId(), req.getSeverity());
        return result;
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
        docRepo.findByIssueIdOrderByUploadedAtDesc(id)
            .forEach(d -> { try { Files.deleteIfExists(Path.of(d.getFilePath())); } catch (Exception ignored) {} });
        docRepo.deleteByIssueId(id);
        issueRepo.deleteById(id);
        log.info("Issue supprimée : id={}", id);
    }

    // ── Pièces jointes (PDF : PDCA, 8D...) ───────────────────────────
    @Transactional
    public IssueDocumentDto uploadDocument(Long issueId, MultipartFile file) throws IOException {
        if (!issueRepo.existsById(issueId)) {
            throw new IllegalArgumentException("Issue introuvable: " + issueId);
        }
        Path dir = Path.of(UPLOAD_DIR + issueId);
        Files.createDirectories(dir);
        String safeFileName = System.currentTimeMillis() + "_" + sanitize(file.getOriginalFilename());
        Path filePath = dir.resolve(safeFileName);
        Files.write(filePath, file.getBytes());

        IssueDocument doc = IssueDocument.builder()
            .issueId(issueId)
            .fileName(file.getOriginalFilename())
            .filePath(filePath.toString())
            .uploadedAt(LocalDateTime.now())
            .build();
        return toDocDto(docRepo.save(doc));
    }

    public byte[] downloadDocument(Long docId) throws IOException {
        IssueDocument doc = docRepo.findById(docId)
            .orElseThrow(() -> new IllegalArgumentException("Document introuvable: " + docId));
        return Files.readAllBytes(Path.of(doc.getFilePath()));
    }

    public String getDocumentFileName(Long docId) {
        return docRepo.findById(docId).map(IssueDocument::getFileName).orElse("document.pdf");
    }

    @Transactional
    public void deleteDocument(Long docId) {
        IssueDocument doc = docRepo.findById(docId)
            .orElseThrow(() -> new IllegalArgumentException("Document introuvable: " + docId));
        try { Files.deleteIfExists(Path.of(doc.getFilePath())); } catch (Exception ignored) {}
        docRepo.delete(doc);
    }

    private String sanitize(String name) {
        if (name == null) return "file.pdf";
        return name.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private IssueDocumentDto toDocDto(IssueDocument d) {
        IssueDocumentDto dto = new IssueDocumentDto();
        dto.setId(d.getId());
        dto.setFileName(d.getFileName());
        dto.setUploadedAt(d.getUploadedAt());
        return dto;
    }

    private ProjectIssueDto toDto(ProjectIssue i) {
        return toDto(i, docRepo.findByIssueIdOrderByUploadedAtDesc(i.getId()).stream()
            .map(this::toDocDto).collect(Collectors.toList()));
    }

    private ProjectIssueDto toDto(ProjectIssue i, List<IssueDocumentDto> documents) {
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
            .documents(documents)
            .build();
    }
}
