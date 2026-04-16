package com.segula.saasgestion.repository;

import com.segula.saasgestion.domain.ProjectMonthlyForecast;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectMonthlyForecastRepository extends JpaRepository<ProjectMonthlyForecast, Long> {
    List<ProjectMonthlyForecast> findByProjectIdOrderByMonthAsc(Long projectId);
    void deleteByProjectId(Long projectId);
}
