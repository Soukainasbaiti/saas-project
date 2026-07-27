package com.segula.saasgestion.repository;

import com.segula.saasgestion.domain.ProjectCountryForecast;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectCountryForecastRepository extends JpaRepository<ProjectCountryForecast, Long> {
    List<ProjectCountryForecast> findByProjectIdOrderByCountryIdAscMonthAsc(Long projectId);
    Optional<ProjectCountryForecast> findByProjectIdAndCountryIdAndMonth(Long projectId, Long countryId, String month);
    boolean existsByProjectIdAndCountryId(Long projectId, Long countryId);
    void deleteByProjectId(Long projectId);
}
