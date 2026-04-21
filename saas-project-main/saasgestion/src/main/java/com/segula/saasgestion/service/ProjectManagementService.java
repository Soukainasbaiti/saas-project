package com.segula.saasgestion.service;

import com.segula.saasgestion.domain.*;
import com.segula.saasgestion.dto.*;
import com.segula.saasgestion.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectManagementService {

    private final ProjectRepository projectRepository;
    private final ProjectResourceRepository resourceRepo;
    private final ProjectResourceEntryRepository entryRepo;
    private final ProjectOtherCostRepository otherCostRepo;
    private final ProjectManagementConfigRepository configRepo;


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
                .contractType(r.getContractType())
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

        String currency = configRepo.findById(projectId)
            .map(ProjectManagementConfig::getCurrency)
            .orElse("EUR");

        return ProjectManagementDto.builder()
            .projectId(projectId)
            .projectName(project.getProjectName() != null ? project.getProjectName() : project.getActivity())
            .granularity(granularity)
            .granularityLocked(locked)
            .currency(currency)
            .months(periods)
            .resources(resourceDtos)
            .otherCosts(otherCostDtos)
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

    public boolean isGranularityLocked(Long projectId) {
        // Locked as soon as the user explicitly confirmed a granularity choice
        return configRepo.existsById(projectId);
    }

    // ── Resources ──────────────────────────────────────────────────
    @Transactional
    public void addResource(AddResourceRequest req) {
        Project project = projectRepository.findById(req.getProjectId())
            .orElseThrow(() -> new RuntimeException("Project not found"));
        ProjectResource resource = ProjectResource.builder()
            .project(project)
            .matricule(req.getMatricule())
            .personName(req.getPersonName())
            .contractType(req.getContractType())
            .isActive(true)
            .build();
        resourceRepo.save(resource);
    }

    @Transactional
    public void deleteResource(Long resourceId) {
        resourceRepo.deleteById(resourceId);
    }

    @Transactional
    public void updateResourceContractType(Long resourceId, String contractType) {
        ProjectResource resource = resourceRepo.findById(resourceId)
            .orElseThrow(() -> new RuntimeException("Resource not found"));
        resource.setContractType(contractType);
        resourceRepo.save(resource);
    }

    @Transactional
    public void saveResourceEntry(SaveResourceEntryRequest req) {
        ProjectResource resource = resourceRepo.findById(req.getResourceId())
            .orElseThrow(() -> new RuntimeException("Resource not found"));
        ProjectResourceEntry entry = entryRepo
            .findByResourceIdAndMonth(req.getResourceId(), req.getMonth())
            .orElse(ProjectResourceEntry.builder().resource(resource).month(req.getMonth()).build());
        if (req.getDailyCost()  != null) entry.setDailyCost(req.getDailyCost());
        if (req.getWorkedDays() != null) entry.setWorkedDays(req.getWorkedDays());
        if (req.getBilledDays() != null) entry.setBilledDays(req.getBilledDays());
        if (req.getDailyRate()  != null) entry.setDailyRate(req.getDailyRate());
        entryRepo.save(entry);
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
}
