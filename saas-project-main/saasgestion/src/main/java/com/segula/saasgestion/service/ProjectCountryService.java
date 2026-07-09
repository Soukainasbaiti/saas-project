package com.segula.saasgestion.service;

import com.segula.saasgestion.domain.*;
import com.segula.saasgestion.dto.AddProjectCountryRequest;
import com.segula.saasgestion.dto.BackOfficeCountryRequest;
import com.segula.saasgestion.dto.ProjectCountryDto;
import com.segula.saasgestion.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProjectCountryService {

    private final ProjectRepository        projectRepo;
    private final ProjectCountryRepository projectCountryRepo;
    private final CountryRepository        countryRepo;
    private final AppUserRepository        userRepo;
    private final EmailService             emailService;

    @Transactional(readOnly = true)
    public List<ProjectCountryDto> listByProject(Long projectId) {
        return projectCountryRepo.findByProjectIdOrderByDisplayOrderAsc(projectId).stream()
                .map(this::toDto)
                .toList();
    }

    // Appelee a la creation du projet : pays Front Office (obligatoire, chef de file)
    // + pays Back Office (optionnels, saisis immediatement ou laisses "a assigner").
    @Transactional
    public void initializeCountries(Project project, Long frontOfficeCountryId,
                                     List<BackOfficeCountryRequest> backOfficeCountries,
                                     Long createdByUserId) {
        Country frontCountry = countryRepo.findById(frontOfficeCountryId)
                .orElseThrow(() -> new IllegalArgumentException("Pays Front Office introuvable"));
        AppUser createdBy = userRepo.findById(createdByUserId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        ProjectCountry lead = ProjectCountry.builder()
                .project(project)
                .country(frontCountry)
                .pm(project.getProjectManager()) // le PM du projet = PM du pays chef de file
                .isLead(true)
                .displayOrder(1)
                .addedBy(createdBy)
                .build();
        projectCountryRepo.save(lead);

        int order = 2;
        if (backOfficeCountries != null) {
            for (BackOfficeCountryRequest bo : backOfficeCountries) {
                if (bo.getCountryId().equals(frontOfficeCountryId)) continue; // deja le pays chef de file
                Country country = countryRepo.findById(bo.getCountryId())
                        .orElseThrow(() -> new IllegalArgumentException("Pays introuvable: " + bo.getCountryId()));
                AppUser pm = bo.getPmId() != null
                        ? userRepo.findById(bo.getPmId()).orElseThrow(() -> new IllegalArgumentException("PM introuvable"))
                        : null;

                ProjectCountry pc = ProjectCountry.builder()
                        .project(project)
                        .country(country)
                        .pm(pm)
                        .isLead(false)
                        .displayOrder(order++)
                        .addedBy(createdBy)
                        .build();
                projectCountryRepo.save(pc);

                notifyCountryAdded(pc, createdBy);
            }
        }

        recomputeProjectName(project);
    }

    @Transactional
    public ProjectCountryDto addCountry(Long projectId, AddProjectCountryRequest req, Long requestedByUserId) {
        Project project = projectRepo.findById(projectId)
                .orElseThrow(() -> new IllegalArgumentException("Projet introuvable"));
        Country country = countryRepo.findById(req.getCountryId())
                .orElseThrow(() -> new IllegalArgumentException("Pays introuvable"));

        if (projectCountryRepo.existsByProjectIdAndCountryId(projectId, req.getCountryId())) {
            throw new IllegalArgumentException("Ce pays est deja rattache a ce projet.");
        }

        AppUser pm = req.getPmId() != null
                ? userRepo.findById(req.getPmId()).orElseThrow(() -> new IllegalArgumentException("PM introuvable"))
                : null;

        AppUser addedBy = userRepo.findById(requestedByUserId)
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur introuvable"));

        int nextOrder = projectCountryRepo.findByProjectIdOrderByDisplayOrderAsc(projectId).size() + 1;

        ProjectCountry pc = ProjectCountry.builder()
                .project(project)
                .country(country)
                .pm(pm)
                .isLead(false) // le pays chef de file est fixe uniquement a la creation du projet
                .displayOrder(nextOrder)
                .addedBy(addedBy)
                .build();
        projectCountryRepo.save(pc);

        recomputeProjectName(project);

        notifyCountryAdded(pc, addedBy);

        log.info("Pays {} ajoute au projet {} par {}", country.getName(), projectId, addedBy.getFullName());
        return toDto(pc);
    }

    @Transactional
    public ProjectCountryDto assignPm(Long projectId, Long countryId, Long pmId) {
        ProjectCountry pc = projectCountryRepo.findByProjectIdOrderByDisplayOrderAsc(projectId).stream()
                .filter(c -> c.getCountry().getId().equals(countryId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Ce pays n'est pas rattache a ce projet."));

        boolean wasUnassigned = pc.getPm() == null;

        AppUser pm = userRepo.findById(pmId)
                .orElseThrow(() -> new IllegalArgumentException("PM introuvable"));
        pc.setPm(pm);
        projectCountryRepo.save(pc);

        if (wasUnassigned) {
            notifyPmAssigned(pc);
        }

        log.info("PM {} assigne au pays {} du projet {}", pm.getFullName(), countryId, projectId);
        return toDto(pc);
    }

    // ── Notifications ────────────────────────────────────────────────
    private void notifyCountryAdded(ProjectCountry pc, AppUser addedBy) {
        if (pc.getPm() != null) {
            notifyPmAssigned(pc);
        }
        String pmName = pc.getPm() != null ? pc.getPm().getFullName() : null;
        for (AppUser admin : userRepo.findAllActiveAdmins()) {
            emailService.sendCountryAddedToAdmin(
                    admin.getEmail(), admin.getFullName(),
                    projectDisplayName(pc.getProject()), pc.getProject().getProjectCode(),
                    pc.getCountry().getName(),
                    addedBy.getFullName(), pmName,
                    pc.getProject().getId()
            );
        }
    }

    private void notifyPmAssigned(ProjectCountry pc) {
        AppUser pm = pc.getPm();
        if (pm == null) return;
        AppUser frontOfficePm = projectCountryRepo.findByProjectIdAndIsLeadTrue(pc.getProject().getId())
                .map(ProjectCountry::getPm)
                .orElse(pc.getProject().getProjectManager());

        emailService.sendCountryPmAssigned(
                pm.getEmail(), pm.getFullName(),
                projectDisplayName(pc.getProject()), pc.getProject().getProjectCode(),
                pc.getCountry().getName(), pc.getCountry().getIsoCode(),
                frontOfficePm != null ? frontOfficePm.getFullName() : "l'administrateur",
                frontOfficePm != null ? frontOfficePm.getEmail() : "",
                pc.getProject().getId()
        );
    }

    // ── Nom de projet auto-genere : suffixe des codes pays (ordonne) ──
    private void recomputeProjectName(Project project) {
        List<ProjectCountry> countries = projectCountryRepo.findByProjectIdOrderByDisplayOrderAsc(project.getId());
        if (countries.isEmpty()) return;

        String suffix = countries.stream()
                .sorted(Comparator.comparingInt(ProjectCountry::getDisplayOrder))
                .map(pc -> pc.getCountry().getIsoCode())
                .reduce((a, b) -> a + "-" + b)
                .orElse("");

        String baseName = stripExistingCountrySuffix(project.getProjectName(), countries);
        project.setProjectName(baseName + "-" + suffix);
        projectRepo.save(project);
    }

    private String stripExistingCountrySuffix(String currentName, List<ProjectCountry> countries) {
        if (currentName == null) return "";
        String name = currentName;
        for (ProjectCountry pc : countries) {
            String tag = "-" + pc.getCountry().getIsoCode();
            if (name.endsWith(tag)) {
                name = name.substring(0, name.length() - tag.length());
            }
        }
        return name;
    }

    private String projectDisplayName(Project project) {
        return project.getProjectName() != null ? project.getProjectName() : project.getActivity();
    }

    private ProjectCountryDto toDto(ProjectCountry pc) {
        return ProjectCountryDto.builder()
                .id(pc.getId())
                .countryId(pc.getCountry().getId())
                .countryName(pc.getCountry().getName())
                .countryIsoCode(pc.getCountry().getIsoCode())
                .pmId(pc.getPm() != null ? pc.getPm().getId() : null)
                .pmName(pc.getPm() != null ? pc.getPm().getFullName() : null)
                .pmEmail(pc.getPm() != null ? pc.getPm().getEmail() : null)
                .isLead(pc.isLead())
                .displayOrder(pc.getDisplayOrder())
                .build();
    }
}
