package com.segula.saasgestion.repository;

import com.segula.saasgestion.domain.ProjectMonthStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectMonthStatusRepository extends JpaRepository<ProjectMonthStatus, Long> {
    List<ProjectMonthStatus> findByProjectId(Long projectId);
    Optional<ProjectMonthStatus> findByProjectIdAndPeriod(Long projectId, String period);
    void deleteByProjectId(Long projectId);
}
