package com.segula.saasgestion.repository;

import com.segula.saasgestion.domain.ProjectWorkType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectWorkTypeRepository extends JpaRepository<ProjectWorkType, Long> {
    List<ProjectWorkType> findByProjectIdOrderByIdAsc(Long projectId);
}
