package com.segula.saasgestion.repository;

import com.segula.saasgestion.domain.ProjectRisk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProjectRiskRepository extends JpaRepository<ProjectRisk, Long> {

    @Query("SELECT r FROM ProjectRisk r WHERE r.projectId = :projectId ORDER BY r.rId ASC")
    List<ProjectRisk> findByProjectId(@Param("projectId") Long projectId);

    int countByProjectId(Long projectId);
}
