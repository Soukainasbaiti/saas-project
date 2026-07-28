package com.segula.saasgestion.security;

import com.segula.saasgestion.domain.Project;
import com.segula.saasgestion.repository.ProjectCountryRepository;
import com.segula.saasgestion.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

/**
 * Verifie qu'un utilisateur authentifie a un lien reel avec un projet donne
 * avant d'autoriser un acces (lecture ou ecriture) a ses sous-ressources
 * (issues, risks, opportunities, mips, gestion, wip...). Sans ce garde-fou,
 * n'importe quel utilisateur connecte pouvait agir sur n'importe quel
 * projectId en le devinant (IDOR).
 */
@Component("projectAccessGuard")
@RequiredArgsConstructor
public class ProjectAccessGuard {

    private final ProjectRepository projectRepo;
    private final ProjectCountryRepository projectCountryRepo;

    public boolean hasAccess(Long projectId, Authentication authentication) {
        if (authentication == null || projectId == null) return false;

        boolean isAdmin = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_ADMIN"::equals);
        if (isAdmin) return true;

        Long userId = (Long) authentication.getPrincipal();

        Project project = projectRepo.findById(projectId).orElse(null);
        if (project == null) return false;

        if (project.getProjectManager() != null && userId.equals(project.getProjectManager().getId())) return true;
        if (project.getCreatedById() != null && userId.equals(project.getCreatedById())) return true;

        return !projectCountryRepo.findByProjectIdAndPmId(projectId, userId).isEmpty();
    }
}
