package com.segula.saasgestion.service;

import com.segula.saasgestion.domain.*;
import com.segula.saasgestion.dto.*;
import com.segula.saasgestion.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectManagementService {

    private final ProjectRepository projectRepository;
    private final ProjectResourceRepository resourceRepo;
    private final ProjectResourceEntryRepository entryRepo;
    private final ProjectOtherCostRepository otherCostRepo;
    private final ProjectManagementConfigRepository configRepo;
    private final ProjectValidationHistoryRepository historyRepo;
    private final ProjectDeliverableRepository deliverableRepo;
    private final EngagementRepository engagementRepository;
    private final ProjectWorkTypeRepository workTypeRepo;
    private final ProjectWorkTicketRepository workTicketRepo;
    private final ProjectMonthStatusRepository monthStatusRepo;
    private final ProjectMonthlyForecastRepository monthlyForecastRepo;
    private final AppUserRepository appUserRepository;
    private final CountryRepository countryRepository;
    private final ProjectCountryRepository projectCountryRepository;
    private final EmailService emailService;

    // ── Autorisation multi-pays ──────────────────────────────────────
    // Un PM voit toutes les donnees du projet mais ne modifie que les lignes de son pays.
    // L'ADMIN n'est jamais restreint.
    private void assertCanEditResource(ProjectResource resource, Long userId) {
        if (userId == null) return; // appels systeme / sans contexte utilisateur
        String role = appUserRepository.findById(userId).map(AppUser::getRole).orElse("");
        if ("ADMIN".equals(role)) return;

        boolean allowed = projectCountryRepository
                .findByProjectIdAndPmId(resource.getProject().getId(), userId)
                .stream()
                .anyMatch(pc -> pc.getCountry().getId().equals(resource.getCountry().getId()));

        if (!allowed) {
            throw new RuntimeException("Vous ne pouvez modifier que les lignes de votre pays sur ce projet.");
        }
    }


    // ── Get full management DTO ────────────────────────────────────
    public ProjectManagementDto getProjectManagement(Long projectId) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found"));

        String granularity = getGranularity(projectId);
        boolean locked = isGranularityLocked(projectId);
        List<String> periods = generatePeriods(project, granularity);

        List<ProjectResource> resources = resourceRepo.findByProjectIdOrderByIdAsc(projectId);
        List<Long> resourceIds = resources.stream().map(ProjectResource::getId).collect(Collectors.toList());

        // Load all entries
        Map<Long, Map<String, ProjectResourceEntry>> entriesByResource = new HashMap<>();
        if (!resourceIds.isEmpty()) {
            entryRepo.findByResourceIdIn(resourceIds).forEach(e ->
                entriesByResource
                    .computeIfAbsent(e.getResource().getId(), k -> new HashMap<>())
                    .put(e.getMonth(), e)
            );
        }

        // Build resource DTOs
        List<ProjectResourceDto> resourceDtos = resources.stream().map(r -> {
            Map<String, ProjectResourceEntry> entries = entriesByResource.getOrDefault(r.getId(), new HashMap<>());
            Map<String, BigDecimal> dailyCosts  = new LinkedHashMap<>();
            Map<String, BigDecimal> workedDays  = new LinkedHashMap<>();
            Map<String, BigDecimal> billedDays  = new LinkedHashMap<>();
            Map<String, BigDecimal> dailyRates  = new LinkedHashMap<>();
            for (String p : periods) {
                ProjectResourceEntry e = entries.get(p);
                dailyCosts.put(p, e != null ? e.getDailyCost()  : BigDecimal.ZERO);
                workedDays.put(p, e != null ? e.getWorkedDays() : BigDecimal.ZERO);
                billedDays.put(p, e != null ? e.getBilledDays() : BigDecimal.ZERO);
                dailyRates.put(p, e != null ? e.getDailyRate()  : BigDecimal.ZERO);
            }
            return ProjectResourceDto.builder()
                .id(r.getId())
                .matricule(r.getMatricule())
                .personName(r.getPersonName())
                .countryId(r.getCountry() != null ? r.getCountry().getId() : null)
                .countryName(r.getCountry() != null ? r.getCountry().getName() : null)
                .countryIsoCode(r.getCountry() != null ? r.getCountry().getIsoCode() : null)
                .isActive(r.isActive())
                .dailyCosts(dailyCosts)
                .workedDays(workedDays)
                .billedDays(billedDays)
                .dailyRates(dailyRates)
                .build();
        }).collect(Collectors.toList());

        // Load other costs — only categories explicitly created by user
        List<ProjectOtherCost> otherCosts = otherCostRepo.findByProjectIdOrderByCategoryAscMonthAsc(projectId);
        Map<String, Map<String, BigDecimal>> costsByCategory = new LinkedHashMap<>();
        Map<String, Boolean> rebillByCategory = new HashMap<>();

        otherCosts.forEach(oc -> {
            costsByCategory
                .computeIfAbsent(oc.getCategory(), k -> new LinkedHashMap<>())
                .put(oc.getMonth(), oc.getAmount());
            if (oc.isRebill()) rebillByCategory.put(oc.getCategory(), true);
        });

        List<ProjectOtherCostDto> otherCostDtos = costsByCategory.entrySet().stream().map(e -> {
            Map<String, BigDecimal> amounts = new LinkedHashMap<>();
            for (String p : periods) {
                amounts.put(p, e.getValue().getOrDefault(p, BigDecimal.ZERO));
            }
            return ProjectOtherCostDto.builder()
                .category(e.getKey())
                .amounts(amounts)
                .isRebill(rebillByCategory.getOrDefault(e.getKey(), false))
                .build();
        }).collect(Collectors.toList());

        // Statut Réel/Prévision par mois (uniquement les valeurs explicitement choisies par le PM)
        Map<String, String> monthStatusMap = monthStatusRepo.findByProjectId(projectId).stream()
            .collect(Collectors.toMap(ProjectMonthStatus::getPeriod, ProjectMonthStatus::getStatus));

        // Prévisions budgétaires mensuelles (revenue/cost/cov) pour le One Pager
        Map<String, MonthlyForecastDto> monthlyForecastsMap = monthlyForecastRepo.findByProjectIdOrderByMonthAsc(projectId).stream()
            .collect(Collectors.toMap(ProjectMonthlyForecast::getMonth, f -> MonthlyForecastDto.builder()
                .month(f.getMonth())
                .revenue(f.getRevenue())
                .cost(f.getCost())
                .cov(f.getCov())
                .build()));

        ProjectManagementConfig config = configRepo.findById(projectId).orElse(null);
        String currency         = config != null ? config.getCurrency()         : "EUR";
        String validationStatus = config != null ? config.getValidationStatus() : "DRAFT";
        String validatedBy      = config != null ? config.getValidatedBy()      : null;
        OffsetDateTime validatedAt  = config != null ? config.getValidatedAt()  : null;
        String rejectionComment = config != null ? config.getRejectionComment() : null;
        String bumName = project.getBu().getBumName();

        // One Pager : Health & Delivery (saisie PM)
        String deliveryConfidenceLevel  = config != null ? config.getDeliveryConfidenceLevel()  : null;
        Integer healthScoreValue        = config != null ? config.getHealthScoreValue()         : null;
        String healthScoreStatus        = config != null ? config.getHealthScoreStatus()        : null;
        String pmRemarks                = config != null ? config.getPmRemarks()                : null;
        String varianceActualComment    = config != null ? config.getVarianceActualComment()    : null;
        String varianceTrendComment     = config != null ? config.getVarianceTrendComment()     : null;
        String varianceLandingComment   = config != null ? config.getVarianceLandingComment()   : null;
        String tops                     = config != null ? config.getTops()                     : null;
        String flops                    = config != null ? config.getFlops()                    : null;

        return ProjectManagementDto.builder()
            .projectId(projectId)
            .projectName(project.getProjectName() != null ? project.getProjectName() : project.getActivity())
            .granularity(granularity)
            .granularityLocked(locked)
            .currency(currency)
            .months(periods)
            .monthStatus(monthStatusMap)
            .monthlyForecasts(monthlyForecastsMap)
            .startDate(project.getStartDate())
            .endDate(project.getEndDate())
            .resources(resourceDtos)
            .otherCosts(otherCostDtos)
            .validationStatus(validationStatus)
            .validatedBy(validatedBy)
            .validatedAt(validatedAt)
            .rejectionComment(rejectionComment)
            .bumName(bumName)
            .engagementType(project.getEngagement() != null ? project.getEngagement().getEngagementType() : null)
            .clientName(project.getCustomer() != null ? project.getCustomer().getName() : "")
            .buTrigram(project.getBu() != null ? project.getBu().getTrigram() : "")
            .pmName(project.getProjectManager() != null ? project.getProjectManager().getFullName() : "")
            .projectCode(project.getProjectCode() != null ? project.getProjectCode() : "")
            .projectBusinessId(project.getProjectId() != null ? project.getProjectId() : "")
            .deliveryConfidenceLevel(deliveryConfidenceLevel)
            .healthScoreValue(healthScoreValue)
            .healthScoreStatus(healthScoreStatus)
            .pmRemarks(pmRemarks)
            .varianceActualComment(varianceActualComment)
            .varianceTrendComment(varianceTrendComment)
            .varianceLandingComment(varianceLandingComment)
            .tops(tops)
            .flops(flops)
            .build();
    }

    // ── Granularity & Currency ─────────────────────────────────────
    public String getGranularity(Long projectId) {
        return configRepo.findById(projectId)
            .map(ProjectManagementConfig::getGranularity)
            .orElse("MONTHLY");
    }

    @Transactional
    public void setGranularity(Long projectId, String granularity, String currency) {
        if (isGranularityLocked(projectId)) {
            throw new RuntimeException("Granularity cannot be changed after it has been confirmed");
        }
        ProjectManagementConfig config = configRepo.findById(projectId)
            .orElse(ProjectManagementConfig.builder().projectId(projectId).build());
        config.setGranularity(granularity.toUpperCase());
        config.setCurrency(currency != null ? currency.toUpperCase() : "EUR");
        configRepo.save(config);
    }

    // ── One Pager : Health & Delivery (saisie PM) ──────────────────
    @Transactional
    public void updateOnePagerExtras(Long projectId, ProjectManagementDto req) {
        ProjectManagementConfig config = configRepo.findById(projectId)
            .orElse(ProjectManagementConfig.builder().projectId(projectId).build());
        config.setDeliveryConfidenceLevel(req.getDeliveryConfidenceLevel());
        config.setHealthScoreValue(req.getHealthScoreValue());
        config.setHealthScoreStatus(req.getHealthScoreStatus());
        config.setPmRemarks(req.getPmRemarks());
        config.setVarianceActualComment(req.getVarianceActualComment());
        config.setVarianceTrendComment(req.getVarianceTrendComment());
        config.setVarianceLandingComment(req.getVarianceLandingComment());
        config.setTops(req.getTops());
        config.setFlops(req.getFlops());
        configRepo.save(config);
    }

    public boolean isGranularityLocked(Long projectId) {
        // Locked as soon as the user explicitly confirmed a granularity choice
        return configRepo.existsById(projectId);
    }

    // ── Statut Réel/Prévision par mois ───────────────────────────────
    @Transactional
    public List<MonthStatusDto> updateMonthStatus(Long projectId, List<MonthStatusDto> updates) {
        if (!projectRepository.existsById(projectId)) {
            throw new IllegalArgumentException("Projet introuvable: " + projectId);
        }
        for (MonthStatusDto u : updates) {
            ProjectMonthStatus status = monthStatusRepo.findByProjectIdAndPeriod(projectId, u.getMonth())
                .orElse(ProjectMonthStatus.builder()
                    .projectId(projectId)
                    .period(u.getMonth())
                    .build());
            status.setStatus(u.getStatus());
            monthStatusRepo.save(status);
        }
        return monthStatusRepo.findByProjectId(projectId).stream()
            .map(s -> MonthStatusDto.builder().month(s.getPeriod()).status(s.getStatus()).build())
            .toList();
    }

    // ── Resources ──────────────────────────────────────────────────
    @Transactional
    public void addResource(AddResourceRequest req, Long userId) {
        Project project = projectRepository.findById(req.getProjectId())
            .orElseThrow(() -> new RuntimeException("Project not found"));
        if (req.getMatricule() != null && !req.getMatricule().isBlank()) {
            boolean exists = resourceRepo.findByProjectIdAndMatricule(req.getProjectId(), req.getMatricule()).isPresent();
            if (exists) throw new RuntimeException("Un collaborateur avec le matricule '" + req.getMatricule() + "' existe déjà sur ce projet.");
        }
        Country country = countryRepository.findById(req.getCountryId())
            .orElseThrow(() -> new RuntimeException("Country not found"));
        ProjectResource resource = ProjectResource.builder()
            .project(project)
            .matricule(req.getMatricule())
            .personName(req.getPersonName())
            .country(country)
            .isActive(true)
            .build();
        assertCanEditResource(resource, userId);
        resourceRepo.save(resource);
        log.info("Ressource ajoutée : projet={} nom={}", req.getProjectId(), req.getPersonName());
    }

    @Transactional
    public void deleteResource(Long resourceId, Long userId) {
        ProjectResource resource = resourceRepo.findById(resourceId)
            .orElseThrow(() -> new RuntimeException("Resource not found"));
        assertCanEditResource(resource, userId);
        resourceRepo.deleteById(resourceId);
        log.info("Ressource supprimée : id={}", resourceId);
    }

    @Transactional
    public void updateResourceCountry(Long resourceId, Long countryId, Long userId) {
        ProjectResource resource = resourceRepo.findById(resourceId)
            .orElseThrow(() -> new RuntimeException("Resource not found"));
        assertCanEditResource(resource, userId);
        Country country = countryRepository.findById(countryId)
            .orElseThrow(() -> new RuntimeException("Country not found"));
        resource.setCountry(country);
        resourceRepo.save(resource);
    }

    @Transactional
    public void saveResourceEntry(SaveResourceEntryRequest req, Long userId) {
        ProjectResource resource = resourceRepo.findById(req.getResourceId())
            .orElseThrow(() -> new RuntimeException("Resource not found"));
        assertCanEditResource(resource, userId);
        ProjectResourceEntry entry = entryRepo
            .findByResourceIdAndMonth(req.getResourceId(), req.getMonth())
            .orElse(ProjectResourceEntry.builder().resource(resource).month(req.getMonth()).build());
        if (req.getDailyCost()  != null) entry.setDailyCost(req.getDailyCost());
        if (req.getWorkedDays() != null) entry.setWorkedDays(req.getWorkedDays());
        if (req.getBilledDays() != null) entry.setBilledDays(req.getBilledDays());
        if (req.getDailyRate()  != null) entry.setDailyRate(req.getDailyRate());
        entryRepo.save(entry);
        log.debug("Saisie temps enregistrée : ressource={} mois={}", req.getResourceId(), req.getMonth());
    }

    // ── Other costs ────────────────────────────────────────────────
    @Transactional
    public void saveOtherCost(SaveOtherCostRequest req) {
        Project project = projectRepository.findById(req.getProjectId())
            .orElseThrow(() -> new RuntimeException("Project not found"));
        ProjectOtherCost cost = otherCostRepo
            .findByProjectIdAndCategoryAndMonth(req.getProjectId(), req.getCategory(), req.getMonth())
            .orElse(ProjectOtherCost.builder().project(project)
                .category(req.getCategory()).month(req.getMonth()).build());
        cost.setAmount(req.getAmount() != null ? req.getAmount() : BigDecimal.ZERO);
        cost.setRebill(req.isRebill());
        otherCostRepo.save(cost);
    }

    @Transactional
    public void addCategory(Long projectId, String categoryName) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project not found"));
        // Create a placeholder record with amount 0 to register the category
        boolean exists = otherCostRepo.findByProjectIdAndCategory(projectId, categoryName).stream().findAny().isPresent();
        if (!exists) {
            ProjectOtherCost placeholder = ProjectOtherCost.builder()
                .project(project).category(categoryName).month("0000-00")
                .amount(BigDecimal.ZERO).isRebill(false).build();
            otherCostRepo.save(placeholder);
        }
    }

    @Transactional
    public void deleteCategory(Long projectId, String category) {
        List<ProjectOtherCost> records = otherCostRepo.findByProjectIdAndCategory(projectId, category);
        otherCostRepo.deleteAll(records);
    }

    @Transactional
    public void setCategoryRebill(Long projectId, String category, boolean isRebill) {
        List<ProjectOtherCost> existing = otherCostRepo.findByProjectIdAndCategory(projectId, category);
        if (!existing.isEmpty()) {
            existing.forEach(c -> c.setRebill(isRebill));
            otherCostRepo.saveAll(existing);
        } else {
            Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
            ProjectOtherCost marker = ProjectOtherCost.builder()
                .project(project).category(category).month("0000-00")
                .amount(BigDecimal.ZERO).isRebill(isRebill).build();
            otherCostRepo.save(marker);
        }
    }

    // ── Validation workflow ────────────────────────────────────────
    @Transactional
    public void submitForValidation(Long projectId, String pmName) {
        ProjectManagementConfig config = configRepo.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Project management not initialized"));
        if (!"DRAFT".equals(config.getValidationStatus()) && !"REJECTED".equals(config.getValidationStatus()))
            throw new RuntimeException("Already submitted or validated");
        config.setValidationStatus("SUBMITTED");
        config.setSubmittedAt(OffsetDateTime.now());
        config.setRejectionComment(null);
        configRepo.save(config);
        historyRepo.save(ProjectValidationHistory.builder()
            .projectId(projectId).action("SUBMITTED").actorName(pmName).build());
        log.info("One Pager soumis pour validation : projet={} pm={}", projectId, pmName);

        // Notifier tous les admins actifs par email
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project != null) {
            List<AppUser> admins = appUserRepository.findAllActiveAdmins();
            if (admins.isEmpty()) {
                log.warn("Aucun admin actif trouvé pour notifier la soumission One Pager projet={}", projectId);
            }
            for (AppUser admin : admins) {
                emailService.sendOnePagerSubmittedToAdmin(
                    admin.getEmail(), admin.getFullName(),
                    pmName, projectId,
                    project.getProjectName() != null ? project.getProjectName() : project.getActivity(),
                    project.getRevenueBudget(), project.getCostBudget()
                );
            }
        }
    }

    @Transactional
    public void validateProject(Long projectId, String bumName) {
        ProjectManagementConfig config = configRepo.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Not found"));
        if (!"SUBMITTED".equals(config.getValidationStatus()))
            throw new RuntimeException("Project is not in SUBMITTED state");
        config.setValidationStatus("VALIDATED");
        config.setValidatedAt(OffsetDateTime.now());
        config.setValidatedBy(bumName);
        configRepo.save(config);
        historyRepo.save(ProjectValidationHistory.builder()
            .projectId(projectId).action("VALIDATED").actorName(bumName).build());
        log.info("One Pager validé : projet={} bum={}", projectId, bumName);
    }

    @Transactional
    public void rejectProject(Long projectId, String bumName, String comment) {
        ProjectManagementConfig config = configRepo.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Not found"));
        if (!"SUBMITTED".equals(config.getValidationStatus()))
            throw new RuntimeException("Project is not in SUBMITTED state");
        config.setValidationStatus("REJECTED");
        config.setValidatedBy(bumName);
        config.setRejectionComment(comment);
        configRepo.save(config);
        historyRepo.save(ProjectValidationHistory.builder()
            .projectId(projectId).action("REJECTED").actorName(bumName).comment(comment).build());
        log.warn("One Pager rejeté : projet={} bum={} motif={}", projectId, bumName, comment);
    }

    public List<Map<String, Object>> getValidationHistory() {
        return buildHistoryDtos(historyRepo.findAllOrderByDateDesc());
    }

    // Historique filtré pour un BUM : seulement les projets de sa BU
    public List<Map<String, Object>> getValidationHistoryForBum(String bumName) {
        return historyRepo.findAllOrderByDateDesc().stream()
            .filter(h -> {
                Project p = projectRepository.findById(h.getProjectId()).orElse(null);
                return p != null && p.getBu() != null && bumName.equals(p.getBu().getBumName());
            })
            .map(h -> {
                Project project = projectRepository.findById(h.getProjectId()).orElse(null);
                String projectName = project != null
                    ? (project.getProjectName() != null ? project.getProjectName() : project.getActivity())
                    : "Projet #" + h.getProjectId();
                Map<String, Object> item = new HashMap<>();
                item.put("id",          h.getId());
                item.put("projectId",   h.getProjectId());
                item.put("projectName", projectName);
                item.put("action",      h.getAction());
                item.put("actorName",   h.getActorName());
                item.put("comment",     h.getComment());
                item.put("createdAt",   h.getCreatedAt());
                return item;
            })
            .collect(Collectors.toList());
    }

    private List<Map<String, Object>> buildHistoryDtos(List<ProjectValidationHistory> list) {
        return list.stream().map(h -> {
            Project project = projectRepository.findById(h.getProjectId()).orElse(null);
            String projectName = project != null
                ? (project.getProjectName() != null ? project.getProjectName() : project.getActivity())
                : "Projet #" + h.getProjectId();
            Map<String, Object> item = new HashMap<>();
            item.put("id",          h.getId());
            item.put("projectId",   h.getProjectId());
            item.put("projectName", projectName);
            item.put("action",      h.getAction());
            item.put("actorName",   h.getActorName());
            item.put("comment",     h.getComment());
            item.put("createdAt",   h.getCreatedAt());
            return item;
        }).collect(Collectors.toList());
    }

    public List<Map<String, Object>> getPendingForBum(String bumName) {
        return projectRepository.findAll().stream()
            .filter(p -> p.getDeletedAt() == null)
            .filter(p -> bumName.equals(p.getBu().getBumName()))
            .map(p -> {
                ProjectManagementConfig cfg = configRepo.findById(p.getId()).orElse(null);
                String status = cfg != null ? cfg.getValidationStatus() : "DRAFT";
                Map<String, Object> item = new HashMap<>();
                item.put("projectId", p.getId());
                item.put("projectName", p.getProjectName() != null ? p.getProjectName() : p.getActivity());
                item.put("buName", p.getBu().getName());
                item.put("validationStatus", status);
                item.put("submittedAt", cfg != null ? cfg.getSubmittedAt() : null);
                return item;
            })
            .filter(m -> "SUBMITTED".equals(m.get("validationStatus")))
            .collect(Collectors.toList());
    }

    public List<Map<String, Object>> getRejectedForPm(Long userId) {
        return projectRepository.findRejectedByPmId(userId).stream()
            .map(row -> {
                Map<String, Object> item = new HashMap<>();
                item.put("projectId",        row[0]);
                item.put("projectName",       row[1]);
                item.put("bumName",           row[2]);
                item.put("rejectionComment",  row[3]);
                item.put("rejectedAt",        row[4]);
                return item;
            })
            .collect(Collectors.toList());
    }

    // ── SDH File import ────────────────────────────────────────────
    // Chaque PM importe son propre SDH : les lignes hors de son pays sont ignorees.
    // Pour l'ADMIN, aucune restriction (allowedCountryIds = null).
    private Set<Long> resolveAllowedCountryIds(Long projectId, Long userId) {
        if (userId == null) return null;
        String role = appUserRepository.findById(userId).map(AppUser::getRole).orElse("");
        if ("ADMIN".equals(role)) return null;
        return projectCountryRepository.findByProjectIdAndPmId(projectId, userId).stream()
            .map(pc -> pc.getCountry().getId())
            .collect(Collectors.toSet());
    }

    @Transactional
    public Map<String, Object> importSdhFile(Long projectId, MultipartFile file, Long userId) {
        Set<Long> allowedCountryIds = resolveAllowedCountryIds(projectId, userId);
        String filename = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
            return importSdhExcel(projectId, file, allowedCountryIds);
        }
        return importSdhCsv(projectId, file, allowedCountryIds);
    }

    private Map<String, Object> importSdhCsv(Long projectId, MultipartFile file, Set<Long> allowedCountryIds) {
        int imported = 0; int skipped = 0;
        List<String> errors = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line; boolean firstLine = true;
            while ((line = reader.readLine()) != null) {
                if (firstLine) { firstLine = false; continue; }
                if (line.isBlank()) continue;
                String[] cols = line.split("[;,\t]");
                if (cols.length < 3) { skipped++; continue; }
                var result = processSdhRow(projectId, cols[0].trim(), cols[1].trim(), cols[2].trim(), errors, allowedCountryIds);
                if (result) imported++; else skipped++;
            }
        } catch (Exception e) {
            throw new RuntimeException("Erreur lecture CSV : " + e.getMessage() +
                ". Format attendu : matricule;mois(yyyy-MM);heures");
        }
        return Map.of("imported", imported, "skipped", skipped, "errors", errors);
    }

    private Map<String, Object> importSdhExcel(Long projectId, MultipartFile file, Set<Long> allowedCountryIds) {
        int imported = 0; int skipped = 0;
        List<String> errors = new ArrayList<>();
        try (org.apache.poi.ss.usermodel.Workbook wb =
                org.apache.poi.ss.usermodel.WorkbookFactory.create(file.getInputStream())) {

            // Find the detail sheet: prefer one whose name contains "rapport" or "detail"
            org.apache.poi.ss.usermodel.Sheet sheet = null;
            for (int s = 0; s < wb.getNumberOfSheets(); s++) {
                String name = wb.getSheetName(s).toLowerCase();
                if (name.contains("rapport") || name.contains("detail")) {
                    sheet = wb.getSheetAt(s);
                    break;
                }
            }
            if (sheet == null) sheet = wb.getSheetAt(wb.getNumberOfSheets() - 1);

            // Scan the first 20 rows to find the header and column positions
            int headerRowIdx = -1, colMatricule = -1, colDate = -1, colQty = -1;
            for (int r = 0; r <= Math.min(20, sheet.getLastRowNum()); r++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(r);
                if (row == null) continue;
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    org.apache.poi.ss.usermodel.Cell cell = row.getCell(c);
                    if (cell == null) continue;
                    String val = cellStr(cell).toLowerCase();
                    if (val.contains("matricule"))             { colMatricule = c; headerRowIdx = r; }
                    if (val.contains("date") && val.contains("pointage")) colDate = c;
                    if (val.contains("quantit"))               colQty = c;
                }
                if (headerRowIdx >= 0 && colMatricule >= 0 && colDate >= 0 && colQty >= 0) break;
            }
            if (headerRowIdx < 0 || colMatricule < 0 || colDate < 0 || colQty < 0) {
                throw new RuntimeException(
                    "En-tête SDH introuvable. Colonnes attendues : Matricule, Date de Pointage, Quantité");
            }

            // Pre-load project matricules to silently skip unrelated employees
            // (scope au pays du PM important le fichier, sauf pour l'ADMIN)
            Set<String> projectMatricules = resourceRepo.findByProjectIdOrderByIdAsc(projectId)
                .stream()
                .filter(r -> allowedCountryIds == null || allowedCountryIds.contains(r.getCountry().getId()))
                .map(ProjectResource::getMatricule).filter(Objects::nonNull)
                .collect(Collectors.toSet());

            // Detect project granularity to format period keys correctly
            String granularity = getGranularity(projectId);
            DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            java.time.temporal.WeekFields isoWeek = java.time.temporal.WeekFields.ISO;

            // Aggregate hours by (matricule → periodKey → totalHours)
            Map<String, Map<String, Double>> hoursMap = new LinkedHashMap<>();

            for (int r = headerRowIdx + 1; r <= sheet.getLastRowNum(); r++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(r);
                if (row == null) continue;
                org.apache.poi.ss.usermodel.Cell cMat  = row.getCell(colMatricule);
                org.apache.poi.ss.usermodel.Cell cDate = row.getCell(colDate);
                org.apache.poi.ss.usermodel.Cell cQty  = row.getCell(colQty);
                if (cMat == null || cDate == null || cQty == null) continue;

                String matricule = cellStr(cMat);
                if (matricule.isBlank() || !projectMatricules.contains(matricule)) continue;

                // Parse raw date from cell
                LocalDate date;
                if (cDate.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC
                        && org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cDate)) {
                    date = cDate.getLocalDateTimeCellValue().toLocalDate();
                } else {
                    String dateStr = cellStr(cDate);
                    if (dateStr.isBlank()) continue;
                    try { date = LocalDate.parse(dateStr, dateFmt); }
                    catch (Exception ex) {
                        try { date = LocalDate.parse(dateStr); }
                        catch (Exception ex2) { continue; }
                    }
                }

                // Format period key based on project granularity
                String periodKey;
                if ("WEEKLY".equalsIgnoreCase(granularity)) {
                    int week = date.get(isoWeek.weekOfWeekBasedYear());
                    int year = date.get(isoWeek.weekBasedYear());
                    periodKey = String.format("%04d-W%02d", year, week);
                } else if ("DAILY".equalsIgnoreCase(granularity)) {
                    periodKey = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                } else {
                    periodKey = date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
                }

                double qty;
                try {
                    qty = cQty.getCellType() == org.apache.poi.ss.usermodel.CellType.NUMERIC
                        ? cQty.getNumericCellValue()
                        : Double.parseDouble(cellStr(cQty).replace(",", "."));
                } catch (Exception ex) { continue; }

                hoursMap.computeIfAbsent(matricule, k -> new LinkedHashMap<>())
                        .merge(periodKey, qty, Double::sum);
            }

            // Write aggregated results
            Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Projet introuvable : " + projectId));
            Set<String> projectPeriods = new java.util.HashSet<>(generatePeriods(project, granularity));
            int outOfRange = 0;
            for (Map.Entry<String, Map<String, Double>> me : hoursMap.entrySet()) {
                String matricule = me.getKey();
                for (Map.Entry<String, Double> monthEntry : me.getValue().entrySet()) {
                    String periodKey2 = monthEntry.getKey();
                    if (!projectPeriods.contains(periodKey2)) {
                        outOfRange++;
                        continue;
                    }
                    String hoursStr = String.valueOf(monthEntry.getValue());
                    boolean ok = processSdhRow(projectId, matricule, periodKey2, hoursStr, errors, allowedCountryIds);
                    if (ok) imported++; else skipped++;
                }
            }
            if (outOfRange > 0) {
                errors.add("⚠️ " + outOfRange + " entrée(s) ignorée(s) car hors de la période du projet. Vérifiez les dates de début/fin du projet.");
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Erreur lecture SDH Excel : " + e.getMessage());
        }
        return Map.of("imported", imported, "skipped", skipped, "errors", errors);
    }

    private String cellStr(org.apache.poi.ss.usermodel.Cell cell) {
        return switch (cell.getCellType()) {
            case NUMERIC -> {
                double v = cell.getNumericCellValue();
                // Detect date-formatted cells
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    LocalDate d = cell.getLocalDateTimeCellValue().toLocalDate();
                    yield d.format(DateTimeFormatter.ofPattern("yyyy-MM"));
                }
                yield v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(v);
            }
            case STRING  -> cell.getStringCellValue().trim();
            case FORMULA -> cell.getCachedFormulaResultType() ==
                org.apache.poi.ss.usermodel.CellType.NUMERIC
                    ? String.valueOf((long) cell.getNumericCellValue())
                    : cell.getStringCellValue().trim();
            default -> "";
        };
    }

    private boolean processSdhRow(Long projectId, String matricule, String month, String hoursStr,
                                   List<String> errors, Set<Long> allowedCountryIds) {
        try {
            double hours = Double.parseDouble(hoursStr.replace(",", "."));
            double days  = Math.round(hours / 8.8 * 100.0) / 100.0;
            ProjectResource resource = resourceRepo
                .findByProjectIdAndMatricule(projectId, matricule)
                .orElse(null);
            if (resource == null) {
                errors.add("Matricule introuvable : " + matricule);
                return false;
            }
            if (allowedCountryIds != null && !allowedCountryIds.contains(resource.getCountry().getId())) {
                errors.add("Matricule " + matricule + " hors de votre pays — ligne ignorée.");
                return false;
            }
            ProjectResourceEntry entry = entryRepo
                .findByResourceIdAndMonth(resource.getId(), month)
                .orElse(ProjectResourceEntry.builder().resource(resource).month(month).build());
            entry.setWorkedDays(BigDecimal.valueOf(days));
            entryRepo.save(entry);
            return true;
        } catch (NumberFormatException e) {
            errors.add("Valeur d'heures invalide '" + hoursStr + "' pour " + matricule);
            return false;
        }
    }

    // ── Period generation ──────────────────────────────────────────
    private List<String> generatePeriods(Project project, String granularity) {
        LocalDate start = project.getStartDate() != null ? project.getStartDate() : LocalDate.now().withDayOfMonth(1);
        LocalDate end   = project.getEndDate()   != null ? project.getEndDate()   : start.plusMonths(11);
        return switch (granularity.toUpperCase()) {
            case "WEEKLY"  -> generateWeeks(start, end);
            case "DAILY"   -> generateDays(start, end);
            default        -> generateMonths(start, end);
        };
    }

    private List<String> generateMonths(LocalDate start, LocalDate end) {
        List<String> periods = new ArrayList<>();
        LocalDate cursor = start.withDayOfMonth(1);
        LocalDate endMonth = end.withDayOfMonth(1);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM");
        while (!cursor.isAfter(endMonth)) {
            periods.add(cursor.format(fmt));
            cursor = cursor.plusMonths(1);
        }
        return periods;
    }

    private List<String> generateWeeks(LocalDate start, LocalDate end) {
        List<String> periods = new ArrayList<>();
        WeekFields wf = WeekFields.ISO;
        LocalDate cursor = start.with(wf.dayOfWeek(), 1); // Monday of the week
        while (!cursor.isAfter(end)) {
            int week = cursor.get(wf.weekOfWeekBasedYear());
            int year = cursor.get(wf.weekBasedYear());
            periods.add(String.format("%04d-W%02d", year, week));
            cursor = cursor.plusWeeks(1);
        }
        return periods;
    }

    private List<String> generateDays(LocalDate start, LocalDate end) {
        List<String> periods = new ArrayList<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate cursor = start;
        // Safety limit: max 366 days to avoid huge tables
        int limit = 366;
        while (!cursor.isAfter(end) && limit-- > 0) {
            periods.add(cursor.format(fmt));
            cursor = cursor.plusDays(1);
        }
        return periods;
    }

    // ── Changement type d'engagement ────────────────────────────────
    @Transactional
    public Map<String, String> changeEngagement(Long projectId, String newEngagementType) {
        Project project = projectRepository.findById(projectId)
            .orElseThrow(() -> new RuntimeException("Projet introuvable : " + projectId));

        Engagement engagement = engagementRepository.findAll().stream()
            .filter(e -> e.getEngagementType().equalsIgnoreCase(newEngagementType))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Type d'engagement introuvable : " + newEngagementType));

        project.setEngagement(engagement);
        projectRepository.save(project);
        return Map.of(
            "engagementType", engagement.getEngagementType(),
            "message", "Engagement mis à jour : " + engagement.getEngagementType()
        );
    }

    // ── Work Package — Deliverables ───────────────────────────────────

    public List<ProjectDeliverableDto> getDeliverables(Long projectId) {
        return deliverableRepo.findByProjectIdOrderByLotNameAscDeliverableIdAsc(projectId)
            .stream().map(this::toDeliverableDto).collect(Collectors.toList());
    }

    @Transactional
    public ProjectDeliverableDto createDeliverable(CreateDeliverableRequest req) {
        // Auto-generate deliverable ID: D_{LotName}_0001
        int count = deliverableRepo.countByProjectIdAndLotName(req.getProjectId(), req.getLotName());
        String lotKey = req.getLotName().trim().toUpperCase().replaceAll("[^A-Z0-9]", "");
        if (lotKey.isEmpty()) lotKey = "LOT";
        String delivId = String.format("D_%s_%04d", lotKey, count + 1);

        ProjectDeliverable d = ProjectDeliverable.builder()
            .projectId(req.getProjectId())
            .lotName(req.getLotName())
            .deliverableId(delivId)
            .deliverableName(req.getDeliverableName())
            .discipline(req.getDiscipline())
            .owner(req.getOwner())
            .plannedDate(req.getPlannedDate())
            .plannedRevenue(req.getPlannedRevenue())
            .status("TO_DO")
            .priority(req.getPriority() != null ? req.getPriority() : "MEDIUM")
            .comments(req.getComments())
            .build();

        return toDeliverableDto(deliverableRepo.save(d));
    }

    @Transactional
    public ProjectDeliverableDto updateDeliverable(Long id, UpdateDeliverableRequest req) {
        ProjectDeliverable d = deliverableRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Deliverable not found: " + id));

        if (req.getDeliverableName() != null) d.setDeliverableName(req.getDeliverableName());
        if (req.getDiscipline()      != null) d.setDiscipline(req.getDiscipline());
        if (req.getOwner()           != null) d.setOwner(req.getOwner());
        if (req.getPlannedDate()     != null) d.setPlannedDate(req.getPlannedDate());
        if (req.getStatus()          != null) d.setStatus(req.getStatus());
        if (req.getPlannedRevenue()  != null) d.setPlannedRevenue(req.getPlannedRevenue());
        if (req.getPriority()        != null) d.setPriority(req.getPriority());
        if (req.getComments()        != null) d.setComments(req.getComments());
        if (req.getFirstPass()       != null) d.setFirstPass(req.getFirstPass());
        // deliveryDate can be set to null explicitly, so we always update it
        d.setDeliveryDate(req.getDeliveryDate());
        if (req.getRfRevenue()       != null) d.setRfRevenue(req.getRfRevenue());

        return toDeliverableDto(deliverableRepo.save(d));
    }

    @Transactional
    public void deleteDeliverable(Long id) {
        deliverableRepo.deleteById(id);
    }

    // ── Unit of Work ──────────────────────────────────────────────────

    public List<ProjectWorkTypeDto> getWorkTypes(Long projectId) {
        List<ProjectWorkType> types = workTypeRepo.findByProjectIdOrderByIdAsc(projectId);
        List<ProjectWorkTicket> allTickets = workTicketRepo.findByProjectIdOrderByTicketIdAsc(projectId);
        return types.stream().map(t -> toWorkTypeDto(t, allTickets)).collect(Collectors.toList());
    }

    @Transactional
    public ProjectWorkTypeDto createWorkType(CreateWorkTypeRequest req) {
        ProjectWorkType wt = ProjectWorkType.builder()
            .projectId(req.getProjectId())
            .name(req.getName())
            .unitLabel(req.getUnitLabel())
            .unitPrice(req.getUnitPrice())
            .plannedQty(req.getPlannedQty())
            .durationDays(req.getDurationDays())
            .build();
        ProjectWorkType saved = workTypeRepo.save(wt);
        return toWorkTypeDto(saved, List.of());
    }

    @Transactional
    public void deleteWorkType(Long id) {
        workTypeRepo.deleteById(id);
    }

    public List<ProjectWorkTicketDto> getWorkTickets(Long projectId) {
        return workTicketRepo.findByProjectIdOrderByTicketIdAsc(projectId)
            .stream().map(this::toWorkTicketDto).collect(Collectors.toList());
    }

    @Transactional
    public ProjectWorkTicketDto createWorkTicket(CreateWorkTicketRequest req) {
        ProjectWorkType wt = workTypeRepo.findById(req.getWorkTypeId())
            .orElseThrow(() -> new RuntimeException("Work type not found"));

        int count = workTicketRepo.countByProjectId(req.getProjectId());
        String ticketId = String.format("UW_%04d", count + 1);

        // Auto-calculate end date if start provided but no end
        LocalDate endDate = req.getEndDate();
        if (endDate == null && req.getStartDate() != null && wt.getDurationDays() != null) {
            endDate = req.getStartDate().plusDays(wt.getDurationDays().longValue());
        }

        ProjectWorkTicket ticket = ProjectWorkTicket.builder()
            .projectId(req.getProjectId())
            .ticketId(ticketId)
            .workType(wt)
            .quantity(req.getQuantity() != null ? req.getQuantity() : 1)
            .consultant(req.getConsultant())
            .assignedDate(req.getAssignedDate())
            .startDate(req.getStartDate())
            .endDate(endDate)
            .status("TO_DO")
            .comments(req.getComments())
            .build();

        return toWorkTicketDto(workTicketRepo.save(ticket));
    }

    @Transactional
    public ProjectWorkTicketDto updateWorkTicket(Long id, UpdateWorkTicketRequest req) {
        ProjectWorkTicket t = workTicketRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Ticket not found: " + id));

        if (req.getQuantity()    != null) t.setQuantity(req.getQuantity());
        if (req.getConsultant()  != null) t.setConsultant(req.getConsultant());
        if (req.getAssignedDate()!= null) t.setAssignedDate(req.getAssignedDate());
        if (req.getStartDate()   != null) {
            t.setStartDate(req.getStartDate());
            // Auto-recalculate end date when start changes if no manual end set
            if (req.getEndDate() == null && t.getWorkType().getDurationDays() != null) {
                t.setEndDate(req.getStartDate().plusDays(t.getWorkType().getDurationDays().longValue()));
            }
        }
        if (req.getEndDate()     != null) t.setEndDate(req.getEndDate());
        if (req.getStatus()      != null) t.setStatus(req.getStatus());
        if (req.getFirstPass()   != null) t.setFirstPass(req.getFirstPass());
        if (req.getComments()    != null) t.setComments(req.getComments());
        t.setDeliveryDate(req.getDeliveryDate());

        return toWorkTicketDto(workTicketRepo.save(t));
    }

    @Transactional
    public void deleteWorkTicket(Long id) {
        workTicketRepo.deleteById(id);
    }

    private ProjectWorkTypeDto toWorkTypeDto(ProjectWorkType wt, List<ProjectWorkTicket> allTickets) {
        int deliveredQty = allTickets.stream()
            .filter(t -> t.getWorkType().getId().equals(wt.getId()) && "DELIVERED".equals(t.getStatus()))
            .mapToInt(t -> t.getQuantity() != null ? t.getQuantity() : 0)
            .sum();

        java.math.BigDecimal unitPrice = wt.getUnitPrice() != null ? wt.getUnitPrice() : java.math.BigDecimal.ZERO;
        java.math.BigDecimal plannedRev = unitPrice.multiply(java.math.BigDecimal.valueOf(wt.getPlannedQty() != null ? wt.getPlannedQty() : 0));
        java.math.BigDecimal actualRev  = unitPrice.multiply(java.math.BigDecimal.valueOf(deliveredQty));
        double completion = wt.getPlannedQty() != null && wt.getPlannedQty() > 0
            ? (deliveredQty * 100.0 / wt.getPlannedQty()) : 0;

        return ProjectWorkTypeDto.builder()
            .id(wt.getId())
            .name(wt.getName())
            .unitLabel(wt.getUnitLabel())
            .unitPrice(wt.getUnitPrice())
            .plannedQty(wt.getPlannedQty())
            .durationDays(wt.getDurationDays())
            .deliveredQty(deliveredQty)
            .plannedRevenue(plannedRev)
            .actualRevenue(actualRev)
            .completionRate(completion)
            .build();
    }

    private ProjectWorkTicketDto toWorkTicketDto(ProjectWorkTicket t) {
        String onTime = "PENDING";
        if (t.getDeliveryDate() != null && t.getEndDate() != null) {
            onTime = t.getDeliveryDate().isAfter(t.getEndDate()) ? "OVERDUE" : "OTD";
        } else if (t.getEndDate() != null && LocalDate.now().isAfter(t.getEndDate())
                && !"DELIVERED".equals(t.getStatus())) {
            onTime = "OVERDUE";
        }

        java.math.BigDecimal revenue = java.math.BigDecimal.ZERO;
        if (t.getWorkType().getUnitPrice() != null && t.getQuantity() != null) {
            revenue = t.getWorkType().getUnitPrice()
                .multiply(java.math.BigDecimal.valueOf(t.getQuantity()));
        }

        return ProjectWorkTicketDto.builder()
            .id(t.getId())
            .ticketId(t.getTicketId())
            .workTypeId(t.getWorkType().getId())
            .workTypeName(t.getWorkType().getName())
            .unitLabel(t.getWorkType().getUnitLabel())
            .quantity(t.getQuantity())
            .consultant(t.getConsultant())
            .assignedDate(t.getAssignedDate())
            .startDate(t.getStartDate())
            .endDate(t.getEndDate())
            .deliveryDate(t.getDeliveryDate())
            .firstPass(t.getFirstPass())
            .onTime(onTime)
            .status(t.getStatus())
            .comments(t.getComments())
            .revenue(revenue)
            .build();
    }

    private ProjectDeliverableDto toDeliverableDto(ProjectDeliverable d) {
        BigDecimal gap = null;
        if (d.getRfRevenue() != null && d.getPlannedRevenue() != null) {
            gap = d.getRfRevenue().subtract(d.getPlannedRevenue());
        }

        String onTime = "PENDING";
        if (d.getDeliveryDate() != null && d.getPlannedDate() != null) {
            onTime = d.getDeliveryDate().isAfter(d.getPlannedDate()) ? "OVERDUE" : "OTD";
        } else if (d.getPlannedDate() != null && LocalDate.now().isAfter(d.getPlannedDate())
                && !"APPROVED".equals(d.getStatus())) {
            onTime = "OVERDUE";
        }

        return ProjectDeliverableDto.builder()
            .id(d.getId())
            .deliverableId(d.getDeliverableId())
            .lotName(d.getLotName())
            .deliverableName(d.getDeliverableName())
            .discipline(d.getDiscipline())
            .owner(d.getOwner())
            .plannedDate(d.getPlannedDate())
            .deliveryDate(d.getDeliveryDate())
            .status(d.getStatus())
            .plannedRevenue(d.getPlannedRevenue())
            .rfRevenue(d.getRfRevenue())
            .gap(gap)
            .firstPass(d.getFirstPass())
            .onTime(onTime)
            .priority(d.getPriority())
            .comments(d.getComments())
            .build();
    }
}
