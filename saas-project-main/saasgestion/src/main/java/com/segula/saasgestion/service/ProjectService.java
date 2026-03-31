package com.segula.saasgestion.service;

import com.segula.saasgestion.domain.*;
import com.segula.saasgestion.dto.*;
import com.segula.saasgestion.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.Year;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository               projectRepo;
    private final AppUserRepository               userRepo;
    private final BuRepository                    buRepo;
    private final CustomerRepository              customerRepo;
    private final IndustryRepository              industryRepo;
    private final EngineeringDisciplineRepository disciplineRepo;
    private final ProjectFunctionRepository       functionRepo;
    private final EngagementRepository            engagementRepo;
    private final FrontFinancierRepository        frontFinancierRepo;

    // ────────────────────────────────────────────────────────────────
    // LISTE DES PROJETS AVEC FILTRES
    // ────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public PagedResponse<ProjectListDto> findAll(
            String buId,
            Long customerId,
            String status,
            Short year,
            String search,
            int page,
            int size
    ) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        String filteredBuId = (buId == null || buId.isBlank()) ? null : buId;
        Short filteredYear  = (year == null || year == 0) ? null : year;

        ProjectStatus filteredStatus =
                (status == null || status.isBlank()) ? null : ProjectStatus.fromDbValue(status.trim());

        Page<Project> result =
                filteredYear == null
                        ? projectRepo.findWithFiltersNoYear(filteredBuId, customerId, filteredStatus, pageable)
                        : projectRepo.findWithFiltersWithYear(filteredBuId, customerId, filteredStatus, filteredYear, pageable);

        return PagedResponse.<ProjectListDto>builder()
                .content(result.getContent().stream().map(this::toListDto).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .last(result.isLast())
                .build();
    }

    // ────────────────────────────────────────────────────────────────
    // DÉTAIL DU PROJET
    // ────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public ProjectDetailDto findById(Long id) {
        return toDetailDto(projectRepo.findByIdWithAllRelations(id)
                .orElseThrow(() -> new IllegalArgumentException("Projet introuvable: " + id)));
    }

    // ────────────────────────────────────────────────────────────────
    // CRÉATION PROJET
    // ────────────────────────────────────────────────────────────────
    @Transactional
    public ProjectDetailDto create(ProjectCreateRequest req) {

        FrontFinancier ff = frontFinancierRepo.findByCodeIgnoreCase(req.getFrontFinancier().trim())
                .orElseThrow(() -> new IllegalArgumentException("Front financier introuvable: " + req.getFrontFinancier()));

        AppUser pm = userRepo.findById(req.getProjectManagerId()).orElseThrow();
        BU bu = buRepo.findById(req.getBuId()).orElseThrow();
        Customer cust = customerRepo.findById(req.getCustomerId()).orElseThrow();
        Industry ind = industryRepo.findById(req.getIndustryId()).orElseThrow();
        EngineeringDiscipline disc = disciplineRepo.findById(req.getEngineeringDisciplineId()).orElseThrow();
        Engagement eng = engagementRepo.findById(req.getEngagementId()).orElseThrow();

        ProjectFunction fn =
                (req.getFunctionName() != null && !req.getFunctionName().isBlank())
                        ? functionRepo.findByNameIgnoreCase(req.getFunctionName().trim()).orElse(null)
                        : null;

        Short year = req.getProjectYear() != null
                ? req.getProjectYear()
                : (short) Year.now().getValue();

        // NOM DU PROJET
        String name = buildName(
                ff.getCode(),
                bu.getId(),
                bu.getTrigram(),
                ind.getTrigram(),
                cust.getTrigram(),
                req.getActivity()
        );

        Project p = Project.builder()
                .projectCode(req.getProjectCode())
                .projectName(name)
                .projectYear(year)
                .projectNameLegacy(req.getProjectNameLegacy())
                .frontFinancier(ff)
                .projectManager(pm)
                .bu(bu)
                .customer(cust)
                .industry(ind)
                .engineeringDiscipline(disc)
                .function(fn)
                .engagement(eng)
                .activity(req.getActivity())

                .revenueBudget(nvl(req.getRevenueBudget()))
                .costBudget(nvl(req.getCostBudget()))

                .startDate(req.getStartDate())
                .endDate(req.getEndDate())

                .majorProject(req.isMajorProject())

                // FIX ICI ✔✔✔
                .technicalOffice(
                        req.getTechnicalOffice() == null
                                ? TechnicalOffice.BACK_OFFICE
                                : TechnicalOffice.fromDbValue(req.getTechnicalOffice().trim())
                )

                // FIX ICI ✔✔✔
                .status(
                        req.getStatus() == null
                                ? ProjectStatus.ON_GOING
                                : ProjectStatus.fromDbValue(req.getStatus().trim())
                )

                .createdById(req.getProjectManagerId())
                .build();

        return toDetailDto(projectRepo.save(p));
    }

    // ────────────────────────────────────────────────────────────────
    // ARCHIVER UN PROJET
    // ────────────────────────────────────────────────────────────────
    @Transactional
    public void archive(Long id) {
        Project p = projectRepo.findById(id).orElseThrow();
        p.setDeletedAt(OffsetDateTime.now());
        projectRepo.save(p);
    }

    // ────────────────────────────────────────────────────────────────
    // STATISTIQUES DASHBOARD
    // ────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats(Short year) {

        if (year == null) year = (short) Year.now().getValue();
        final Short y = year;

        var projects = projectRepo.findAll().stream()
                .filter(p -> p.getDeletedAt() == null && y.equals(p.getProjectYear()))
                .toList();

        long total = projects.size();
        long active = projects.stream().filter(p -> p.getStatus() == ProjectStatus.ON_GOING).count();
        long closed = projects.stream().filter(p -> p.getStatus() == ProjectStatus.CLOSED).count();

        BigDecimal revenue = projects.stream()
                .map(p -> nvl(p.getRevenueBudget()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal cost = projects.stream()
                .map(p -> nvl(p.getCostBudget()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal margin = revenue.subtract(cost);

        BigDecimal avgMargin = revenue.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : margin.divide(revenue, 6, RoundingMode.HALF_UP);

        return DashboardStatsDto.builder()
                .totalProjects(total)
                .activeProjects(active)
                .closedProjects(closed)
                .totalRevenue(revenue)
                .totalCost(cost)
                .totalMargin(margin)
                .avgMarginRate(avgMargin)
                .year(y)
                .build();
    }

    // ────────────────────────────────────────────────────────────────
    // UTILS
    // ────────────────────────────────────────────────────────────────
    private static String buildName(
            String ffCode, String buId, String buTrigram,
            String indTrigram, String custTrigram, String activity
    ) {
        String name = String.join(" - ",
                trim(ffCode),
                trim(buId),
                trim(buTrigram),
                trim(indTrigram),
                trim(custTrigram),
                trim(activity)
        );

        return name.length() > 50 ? name.substring(0, 50) : name;
    }

    private static String trim(String s) {
        return s == null ? "" : s.strip();
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private ProjectListDto toListDto(Project p) {
        return ProjectListDto.builder()
                .id(p.getId())
                .projectCode(p.getProjectCode())
                .projectId(p.getProjectId())
                .projectName(p.getProjectName())
                .projectYear(p.getProjectYear())
                .frontFinancier(p.getFrontFinancier().getCode())
                .buId(p.getBu().getId())
                .buName(p.getBu().getName())
                .buTrigram(p.getBu().getTrigram())
                .customerName(p.getCustomer().getName())
                .customerTrigram(p.getCustomer().getTrigram())
                .activity(p.getActivity())
                .status(p.getStatus().getDbValue())
                .majorProject(p.isMajorProject())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .revenueBudget(p.getRevenueBudget())
                .costBudget(p.getCostBudget())
                .marginBudget(p.getMarginBudget())
                .projectMargin(p.getProjectMargin())
                .build();
    }

    private ProjectDetailDto toDetailDto(Project p) {
        return ProjectDetailDto.builder()
                .id(p.getId())
                .projectCode(p.getProjectCode())
                .projectId(p.getProjectId())
                .projectName(p.getProjectName())
                .projectYear(p.getProjectYear())
                .projectNameLegacy(p.getProjectNameLegacy())

                .frontFinancierId(p.getFrontFinancier().getId())
                .frontFinancier(p.getFrontFinancier().getCode())
                .frontFinancierLabel(p.getFrontFinancier().getLabel())

                .projectManagerId(p.getProjectManager().getId())
                .projectManager(p.getProjectManager().getFullName())
                .pmEmail(p.getProjectManager().getEmail())

                .buId(p.getBu().getId())
                .buName(p.getBu().getName())
                .buTrigram(p.getBu().getTrigram())
                .bumName(p.getBu().getBumName())

                .customerId(p.getCustomer().getId())
                .customerName(p.getCustomer().getName())
                .customerTrigram(p.getCustomer().getTrigram())
                .customerGroup(p.getCustomer().getCustomerGroup())

                .industryId(p.getIndustry().getId())
                .industryName(p.getIndustry().getName())
                .industryTrigram(p.getIndustry().getTrigram())

                .engineeringDisciplineId(p.getEngineeringDiscipline().getId())
                .engineeringDiscipline(p.getEngineeringDiscipline().getName())

                .functionId(p.getFunction() != null ? p.getFunction().getId() : null)
                .functionName(p.getFunction() != null ? p.getFunction().getName() : null)

                .engagementId(p.getEngagement().getId())
                .engagement(p.getEngagement().getName())
                .engagementType(p.getEngagement().getEngagementType())

                .activity(p.getActivity())
                .majorProject(p.isMajorProject())
                .technicalOffice(p.getTechnicalOffice().getDbValue())
                .status(p.getStatus().getDbValue())

                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .revenueBudget(p.getRevenueBudget())
                .costBudget(p.getCostBudget())
                .marginBudget(p.getMarginBudget())
                .projectMargin(p.getProjectMargin())

                .createdById(p.getCreatedById())
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .deletedAt(p.getDeletedAt())
                .build();
    }
}