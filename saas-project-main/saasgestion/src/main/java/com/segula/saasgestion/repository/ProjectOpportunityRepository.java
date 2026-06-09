package com.segula.saasgestion.repository;

import com.segula.saasgestion.domain.ProjectOpportunity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProjectOpportunityRepository extends JpaRepository<ProjectOpportunity, Long> {

    @Query("SELECT o FROM ProjectOpportunity o WHERE o.projectId = :projectId ORDER BY o.oId ASC")
    List<ProjectOpportunity> findByProjectId(@Param("projectId") Long projectId);

    int countByProjectId(Long projectId);
}
