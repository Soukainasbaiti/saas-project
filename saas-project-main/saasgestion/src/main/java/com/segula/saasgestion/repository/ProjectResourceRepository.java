package com.segula.saasgestion.repository;

import com.segula.saasgestion.domain.ProjectResource;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectResourceRepository extends JpaRepository<ProjectResource, Long> {
    List<ProjectResource> findByProjectIdOrderByIdAsc(Long projectId);
}
