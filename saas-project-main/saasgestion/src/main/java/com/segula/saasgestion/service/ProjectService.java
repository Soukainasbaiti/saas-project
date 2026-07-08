package com.segula.saasgestion.service;

import com.segula.saasgestion.domain.*;
import com.segula.saasgestion.dto.*;
import com.segula.saasgestion.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository                  projectRepo;
    private final AppUserRepository                  userRepo;
    private final BuRepository                       buRepo;
    private final CustomerRepository                 customerRepo;
    private final IndustryRepository                 industryRepo;
    private final EngineeringDisciplineRepository    disciplineRepo;
    private final ProjectFunctionRepository          functionRepo;
    private final EngagementRepository               engagementRepo;
    private final FrontFinancierRepository           frontFinancierRepo;
    private final ProjectMonthlyForecastRepository   monthlyForecastRepo;
    private final ProjectCountryService              projectCountryService;

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
            Long createdById,
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
                        ? projectRepo.findWithFiltersNoYear(filteredBuId, customerId, filteredStatus, createdById, pageable)
                        : projectRepo.findWithFiltersWithYear(filteredBuId, customerId, filteredStatus, createdById, filteredYear, pageable);

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
        return create(req, req.getProjectManagerId());
    }

    @Transactional
    public ProjectDetailDto create(ProjectCreateRequest req, Long createdById) {

        FrontFinancier ff = resolveFrontFinancier(req.getFrontFinancier());

        AppUser pm = userRepo.findById(req.getProjectManagerId())
                .orElseThrow(() -> new IllegalArgumentException("Chef de projet introuvable (id=" + req.getProjectManagerId() + ")"));
        BU bu = buRepo.findById(req.getBuId())
                .orElseThrow(() -> new IllegalArgumentException("BU introuvable (id=" + req.getBuId() + ")"));
        Customer cust = customerRepo.findById(req.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Client introuvable (id=" + req.getCustomerId() + ")"));
        Industry ind = industryRepo.findById(req.getIndustryId())
                .orElseThrow(() -> new IllegalArgumentException("Secteur introuvable (id=" + req.getIndustryId() + ")"));
        EngineeringDiscipline disc = disciplineRepo.findById(req.getEngineeringDisciplineId())
                .orElseThrow(() -> new IllegalArgumentException("Discipline introuvable (id=" + req.getEngineeringDisciplineId() + ")"));
        Engagement eng = engagementRepo.findById(req.getEngagementId())
                .orElseThrow(() -> new IllegalArgumentException("Type d'engagement introuvable (id=" + req.getEngagementId() + ")"));

        ProjectFunction fn = null;
        if (req.getFunctionName() != null && !req.getFunctionName().isBlank()) {
            String fname = req.getFunctionName().trim();
            fn = functionRepo.findByNameIgnoreCase(fname)
                    .orElseGet(() -> functionRepo.save(
                            ProjectFunction.builder().name(fname).isActive(true).build()
                    ));
        }

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
        .projectCode(
            (req.getProjectCode() == null || req.getProjectCode().isBlank())
                ? null
                : req.getProjectCode().trim()
        )
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
        .technicalOffice(
            req.getTechnicalOffice() == null
                ? TechnicalOffice.BACK_OFFICE
                : TechnicalOffice.fromDbValue(req.getTechnicalOffice().trim())
        )
        .status(
            req.getStatus() == null
                ? ProjectStatus.ON_GOING
                : ProjectStatus.fromDbValue(req.getStatus().trim())
        )
        .createdById(createdById)
        .build();

        Project saved = projectRepo.save(p);
        saveMonthlyForecasts(saved.getId(), req);
        projectCountryService.initializeCountries(
                saved, req.getFrontOfficeCountryId(), req.getBackOfficeCountries(), createdById
        );
        log.info("Projet créé : id={} code={} pm={}", saved.getId(), saved.getProjectId(), pm.getFullName());
        return toDetailDto(saved);
    }

    private FrontFinancier resolveFrontFinancier(String rawCode) {
        String ffCode = rawCode == null ? "" : rawCode.trim();
        if (ffCode.isEmpty()) {
            throw new IllegalArgumentException("Le champ Front Financier est requis.");
        }
        if (ffCode.length() > 255) {
            throw new IllegalArgumentException(
                    "Le code Front Financier \"" + ffCode + "\" est trop long (" + ffCode.length()
                            + " caractères, 255 maximum).");
        }
        return frontFinancierRepo.findByCodeIgnoreCase(ffCode)
                .orElseGet(() -> frontFinancierRepo.save(
                        FrontFinancier.builder().code(ffCode).label(ffCode).isActive(true).build()
                ));
    }

    private void saveMonthlyForecasts(Long projectId, ProjectCreateRequest req) {
        if (req.getMonthlyForecasts() == null || req.getMonthlyForecasts().isEmpty()) return;
        monthlyForecastRepo.deleteByProjectId(projectId);
        req.getMonthlyForecasts().forEach(f -> monthlyForecastRepo.save(
            ProjectMonthlyForecast.builder()
                .projectId(projectId)
                .month(f.getMonth())
                .revenue(f.getRevenue() != null ? f.getRevenue() : BigDecimal.ZERO)
                .cost(f.getCost()     != null ? f.getCost()     : BigDecimal.ZERO)
                .cov(f.getCov())
                .build()
        ));
    }

    // ────────────────────────────────────────────────────────────────
    // MISE A JOUR DES PREVISIONS MENSUELLES (Revenue / Cost / COV) — apres creation
    // ────────────────────────────────────────────────────────────────
    @Transactional
    public List<MonthlyForecastDto> updateMonthlyForecasts(Long projectId, List<MonthlyForecastDto> updates) {
        if (!projectRepo.existsById(projectId)) {
            throw new IllegalArgumentException("Projet introuvable: " + projectId);
        }

        Map<String, ProjectMonthlyForecast> byMonth = monthlyForecastRepo.findByProjectIdOrderByMonthAsc(projectId)
                .stream()
                .collect(Collectors.toMap(ProjectMonthlyForecast::getMonth, f -> f));

        for (MonthlyForecastDto u : updates) {
            ProjectMonthlyForecast f = byMonth.get(u.getMonth());
            if (f == null) {
                f = ProjectMonthlyForecast.builder()
                        .projectId(projectId)
                        .month(u.getMonth())
                        .revenue(BigDecimal.ZERO)
                        .cost(BigDecimal.ZERO)
                        .build();
            }
            if (u.getRevenue() != null) f.setRevenue(u.getRevenue());
            if (u.getCost()    != null) f.setCost(u.getCost());
            f.setCov(u.getCov());
            monthlyForecastRepo.save(f);
        }

        return monthlyForecastRepo.findByProjectIdOrderByMonthAsc(projectId).stream()
                .map(f -> MonthlyForecastDto.builder()
                        .month(f.getMonth())
                        .revenue(f.getRevenue())
                        .cost(f.getCost())
                        .cov(f.getCov())
                        .build())
                .toList();
    }

    // ────────────────────────────────────────────────────────────────
    // CRÉATION DIRECTE (admin, sans approbation)
    // ────────────────────────────────────────────────────────────────
    @Transactional
    public ProjectDetailDto createDirect(ProjectCreateRequest req, Long adminId) {
        return create(req, adminId);
    }

    // ────────────────────────────────────────────────────────────────
    // MISE À JOUR PROJET (admin)
    // ────────────────────────────────────────────────────────────────
    @Transactional
    public ProjectDetailDto update(Long id, ProjectCreateRequest req) {
        Project p = projectRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Projet introuvable: " + id));

        FrontFinancier ff = resolveFrontFinancier(req.getFrontFinancier());

        AppUser pm   = userRepo.findById(req.getProjectManagerId())
                .orElseThrow(() -> new IllegalArgumentException("Chef de projet introuvable (id=" + req.getProjectManagerId() + ")"));
        BU bu        = buRepo.findById(req.getBuId())
                .orElseThrow(() -> new IllegalArgumentException("BU introuvable (id=" + req.getBuId() + ")"));
        Customer cust = customerRepo.findById(req.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Client introuvable (id=" + req.getCustomerId() + ")"));
        Industry ind  = industryRepo.findById(req.getIndustryId())
                .orElseThrow(() -> new IllegalArgumentException("Secteur introuvable (id=" + req.getIndustryId() + ")"));
        EngineeringDiscipline disc = disciplineRepo.findById(req.getEngineeringDisciplineId())
                .orElseThrow(() -> new IllegalArgumentException("Discipline introuvable (id=" + req.getEngineeringDisciplineId() + ")"));
        Engagement eng = engagementRepo.findById(req.getEngagementId())
                .orElseThrow(() -> new IllegalArgumentException("Type d'engagement introuvable (id=" + req.getEngagementId() + ")"));

        ProjectFunction fn = null;
        if (req.getFunctionName() != null && !req.getFunctionName().isBlank()) {
            String fname = req.getFunctionName().trim();
            fn = functionRepo.findByNameIgnoreCase(fname)
                    .orElseGet(() -> functionRepo.save(
                            ProjectFunction.builder().name(fname).isActive(true).build()));
        }

        p.setFrontFinancier(ff);
        p.setProjectManager(pm);
        p.setBu(bu);
        p.setCustomer(cust);
        p.setIndustry(ind);
        p.setEngineeringDiscipline(disc);
        p.setFunction(fn);
        p.setEngagement(eng);
        p.setActivity(req.getActivity());
        p.setRevenueBudget(nvl(req.getRevenueBudget()));
        p.setCostBudget(nvl(req.getCostBudget()));
        p.setStartDate(req.getStartDate());
        p.setEndDate(req.getEndDate());
        p.setMajorProject(req.isMajorProject());
        if (req.getProjectNameLegacy() != null) p.setProjectNameLegacy(req.getProjectNameLegacy());
        // Mettre à jour projectId seulement si envoyé et non vide ; sinon conserver l'existant
        if (req.getProjectId() != null && !req.getProjectId().trim().isEmpty()) {
            p.setProjectId(req.getProjectId().trim());
        } else if (req.getProjectId() != null && req.getProjectId().trim().isEmpty()) {
            p.setProjectId(null); // l'admin efface volontairement
        }
        p.setTechnicalOffice(req.getTechnicalOffice() == null
                ? TechnicalOffice.BACK_OFFICE
                : TechnicalOffice.fromDbValue(req.getTechnicalOffice().trim()));
        p.setStatus(req.getStatus() == null
                ? ProjectStatus.ON_GOING
                : ProjectStatus.fromDbValue(req.getStatus().trim()));

        ProjectDetailDto result = toDetailDto(projectRepo.save(p));
        log.info("Projet mis à jour : id={} code={}", id, p.getProjectId());
        return result;
    }

    // ────────────────────────────────────────────────────────────────
    // ARCHIVER UN PROJET
    // ────────────────────────────────────────────────────────────────
    @Transactional
    public void archive(Long id) {
        Project p = projectRepo.findById(id).orElseThrow();
        p.setDeletedAt(OffsetDateTime.now());
        projectRepo.save(p);
        log.info("Projet archivé : id={}", id);
    }

    // ────────────────────────────────────────────────────────────────
    // STATISTIQUES DASHBOARD
    // ────────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public DashboardStatsDto getDashboardStats(Short year, Long createdById) {

        if (year == null) year = (short) Year.now().getValue();
        final Short y = year;

        var projects = projectRepo.findAll().stream()
                .filter(p -> p.getDeletedAt() == null && y.equals(p.getProjectYear()))
                .filter(p -> createdById == null || createdById.equals(p.getCreatedById()))
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
                .customerGroup(p.getCustomer().getCustomerGroup())
                .industryTrigram(p.getIndustry().getTrigram())
                .industryId(p.getIndustry().getId())
                .industryName(p.getIndustry().getName())
                .projectManager(p.getProjectManager().getFullName())
                .engagementType(p.getEngagement().getEngagementType())
                .technicalOffice(p.getTechnicalOffice().getDbValue())
                .activity(p.getActivity())
                .status(p.getStatus().getDbValue())
                .majorProject(p.isMajorProject())
                .startDate(p.getStartDate())
                .endDate(p.getEndDate())
                .revenueBudget(p.getRevenueBudget())
                .costBudget(p.getCostBudget())
                .marginBudget(p.getMarginBudget())
                .projectMargin(p.getProjectMargin())
                .countries(toCountryDtos(p))
                .build();
    }

    private List<ProjectCountryDto> toCountryDtos(Project p) {
        return p.getCountries().stream()
                .sorted(java.util.Comparator.comparingInt(ProjectCountry::getDisplayOrder))
                .map(pc -> ProjectCountryDto.builder()
                        .id(pc.getId())
                        .countryId(pc.getCountry().getId())
                        .countryName(pc.getCountry().getName())
                        .countryIsoCode(pc.getCountry().getIsoCode())
                        .pmId(pc.getPm() != null ? pc.getPm().getId() : null)
                        .pmName(pc.getPm() != null ? pc.getPm().getFullName() : null)
                        .isLead(pc.isLead())
                        .displayOrder(pc.getDisplayOrder())
                        .build())
                .toList();
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
                .monthlyForecasts(
                    monthlyForecastRepo.findByProjectIdOrderByMonthAsc(p.getId()).stream()
                        .map(f -> MonthlyForecastDto.builder()
                                .month(f.getMonth())
                                .revenue(f.getRevenue())
                                .cost(f.getCost())
                                .cov(f.getCov())
                                .build())
                        .toList()
                )
                .countries(toCountryDtos(p))
                .build();
    }
}