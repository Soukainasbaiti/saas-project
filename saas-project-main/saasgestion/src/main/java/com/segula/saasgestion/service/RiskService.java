package com.segula.saasgestion.service;

import com.segula.saasgestion.domain.ProjectRisk;
import com.segula.saasgestion.dto.CreateRiskRequest;
import com.segula.saasgestion.dto.ProjectRiskDto;
import com.segula.saasgestion.repository.ProjectRiskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RiskService {

    private final ProjectRiskRepository riskRepo;

    // ── Rating matrix ─────────────────────────────────────────────
    private static final Map<String, Map<String, String>> RATING_MATRIX = Map.of(
        "Very unlikely", Map.of("Negligible","Low","Minor","Low","Moderate","Medium","Significant","High","Severe","High"),
        "Unlikely",      Map.of("Negligible","Low","Minor","Medium","Moderate","Medium","Significant","High","Severe","High"),
        "Possible",      Map.of("Negligible","Medium","Minor","Medium","Moderate","High","Significant","High","Severe","Critical"),
        "Likely",        Map.of("Negligible","Medium","Minor","High","Moderate","High","Significant","Critical","Severe","Critical"),
        "Very Likely",   Map.of("Negligible","High","Minor","High","Moderate","Critical","Significant","Critical","Severe","Critical")
    );

    // ── Probability values ────────────────────────────────────────
    private static final Map<String, Integer> PROB_EVAL = Map.of(
        "Very Likely", 5, "Likely", 4, "Possible", 3, "Unlikely", 2, "Very unlikely", 1
    );
    private static final Map<String, BigDecimal> PROB_PCT = Map.of(
        "Very Likely", new BigDecimal("90"), "Likely", new BigDecimal("70"),
        "Possible", new BigDecimal("50"), "Unlikely", new BigDecimal("30"),
        "Very unlikely", new BigDecimal("10")
    );

    // ── Impact values ─────────────────────────────────────────────
    private static final Map<String, Integer> IMPACT_EVAL = Map.of(
        "Negligible", 1, "Minor", 2, "Moderate", 3, "Significant", 4, "Severe", 5
    );

    // ── CRUD ──────────────────────────────────────────────────────
    public List<ProjectRiskDto> getRisks(Long projectId) {
        return riskRepo.findByProjectId(projectId)
            .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional
    public ProjectRiskDto createRisk(CreateRiskRequest req) {
        int count = riskRepo.countByProjectId(req.getProjectId());
        int year  = req.getIdentificationDate() != null
            ? req.getIdentificationDate().getYear()
            : LocalDate.now().getYear();
        String rId = String.format("R_%04d_%04d", year, count + 1);

        ProjectRisk risk = ProjectRisk.builder()
            .projectId(req.getProjectId())
            .rId(rId)
            .identificationDate(req.getIdentificationDate())
            .phase(req.getPhase())
            .risk(req.getRisk())
            .category(req.getCategory())
            .probability(req.getProbability())
            .probEval(computeProbEval(req.getProbability()))
            .percentProbability(computeProbPct(req.getProbability()))
            .impact(req.getImpact())
            .impactEval(computeImpactEval(req.getImpact()))
            .rating(computeRating(req.getProbability(), req.getImpact()))
            .managementStrategy(req.getManagementStrategy())
            .owner(req.getOwner())
            .mitigationAction(req.getMitigationAction())
            .costs(req.getCosts())
            .probabilityResidual(req.getProbabilityResidual())
            .net(computeNet(req.getCosts(), req.getProbabilityResidual()))
            .contingencyAction(req.getContingencyAction())
            .trigger(req.getTrigger())
            .residualAction(req.getResidualAction())
            .deadline(req.getDeadline())
            .status(req.getStatus() != null ? req.getStatus() : "Identified")
            .closureDate(req.getClosureDate())
            .build();

        return toDto(riskRepo.save(risk));
    }

    @Transactional
    public ProjectRiskDto updateRisk(Long id, CreateRiskRequest req) {
        ProjectRisk risk = riskRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Risk not found: " + id));

        if (req.getIdentificationDate() != null) risk.setIdentificationDate(req.getIdentificationDate());
        if (req.getPhase()              != null) risk.setPhase(req.getPhase());
        if (req.getRisk()               != null) risk.setRisk(req.getRisk());
        if (req.getCategory()           != null) risk.setCategory(req.getCategory());
        if (req.getProbability()        != null) {
            risk.setProbability(req.getProbability());
            risk.setProbEval(computeProbEval(req.getProbability()));
            risk.setPercentProbability(computeProbPct(req.getProbability()));
        }
        if (req.getImpact()             != null) {
            risk.setImpact(req.getImpact());
            risk.setImpactEval(computeImpactEval(req.getImpact()));
        }
        // Recompute rating if prob or impact changed
        risk.setRating(computeRating(risk.getProbability(), risk.getImpact()));

        if (req.getManagementStrategy() != null) risk.setManagementStrategy(req.getManagementStrategy());
        if (req.getOwner()              != null) risk.setOwner(req.getOwner());
        if (req.getMitigationAction()   != null) risk.setMitigationAction(req.getMitigationAction());
        if (req.getCosts()              != null) risk.setCosts(req.getCosts());
        if (req.getProbabilityResidual()!= null) risk.setProbabilityResidual(req.getProbabilityResidual());
        risk.setNet(computeNet(risk.getCosts(), risk.getProbabilityResidual()));
        if (req.getContingencyAction()  != null) risk.setContingencyAction(req.getContingencyAction());
        if (req.getTrigger()            != null) risk.setTrigger(req.getTrigger());
        if (req.getResidualAction()     != null) risk.setResidualAction(req.getResidualAction());
        if (req.getDeadline()           != null) risk.setDeadline(req.getDeadline());
        if (req.getStatus()             != null) risk.setStatus(req.getStatus());
        risk.setClosureDate(req.getClosureDate());

        return toDto(riskRepo.save(risk));
    }

    @Transactional
    public void deleteRisk(Long id) {
        riskRepo.deleteById(id);
    }

    // ── Helpers ───────────────────────────────────────────────────
    private Integer computeProbEval(String prob) {
        return prob != null ? PROB_EVAL.getOrDefault(prob, null) : null;
    }
    private BigDecimal computeProbPct(String prob) {
        return prob != null ? PROB_PCT.getOrDefault(prob, null) : null;
    }
    private Integer computeImpactEval(String impact) {
        return impact != null ? IMPACT_EVAL.getOrDefault(impact, null) : null;
    }
    private String computeRating(String prob, String impact) {
        if (prob == null || impact == null) return null;
        Map<String, String> row = RATING_MATRIX.get(prob);
        return row != null ? row.get(impact) : null;
    }
    private BigDecimal computeNet(BigDecimal costs, BigDecimal residual) {
        if (costs == null || residual == null) return null;
        return costs.multiply(residual).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private ProjectRiskDto toDto(ProjectRisk r) {
        return ProjectRiskDto.builder()
            .id(r.getId()).projectId(r.getProjectId()).rId(r.getRId())
            .identificationDate(r.getIdentificationDate()).phase(r.getPhase()).risk(r.getRisk())
            .category(r.getCategory()).probability(r.getProbability())
            .probEval(r.getProbEval()).percentProbability(r.getPercentProbability())
            .impact(r.getImpact()).impactEval(r.getImpactEval()).rating(r.getRating())
            .managementStrategy(r.getManagementStrategy()).owner(r.getOwner())
            .mitigationAction(r.getMitigationAction()).costs(r.getCosts())
            .probabilityResidual(r.getProbabilityResidual()).net(r.getNet())
            .contingencyAction(r.getContingencyAction()).trigger(r.getTrigger())
            .residualAction(r.getResidualAction()).deadline(r.getDeadline())
            .status(r.getStatus()).closureDate(r.getClosureDate())
            .build();
    }
}
