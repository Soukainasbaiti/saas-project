package com.segula.saasgestion.repository;

import com.segula.saasgestion.domain.ProjectResource;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProjectResourceRepository extends JpaRepository<ProjectResource, Long> {
    List<ProjectResource> findByProjectIdOrderByIdAsc(Long projectId);
    Optional<ProjectResource> findByProjectIdAndMatricule(Long projectId, String matricule);
    boolean existsByProjectIdAndCountryId(Long projectId, Long countryId);
}
