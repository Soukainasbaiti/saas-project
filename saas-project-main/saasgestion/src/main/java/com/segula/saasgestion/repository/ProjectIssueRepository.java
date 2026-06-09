package com.segula.saasgestion.repository;

import com.segula.saasgestion.domain.ProjectIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProjectIssueRepository extends JpaRepository<ProjectIssue, Long> {

    @Query("SELECT i FROM ProjectIssue i WHERE i.projectId = :projectId ORDER BY i.iId ASC")
    List<ProjectIssue> findByProjectId(@Param("projectId") Long projectId);

    int countByProjectId(Long projectId);
}
