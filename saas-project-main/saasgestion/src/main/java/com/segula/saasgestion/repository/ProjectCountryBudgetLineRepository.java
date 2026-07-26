package com.segula.saasgestion.repository;

import com.segula.saasgestion.domain.ProjectCountryBudgetLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectCountryBudgetLineRepository extends JpaRepository<ProjectCountryBudgetLine, Long> {
    List<ProjectCountryBudgetLine> findByProjectIdOrderByCountryIdAscCategoryAscMonthAsc(Long projectId);
    List<ProjectCountryBudgetLine> findByProjectIdAndCountryIdAndCategory(Long projectId, Long countryId, String category);
    Optional<ProjectCountryBudgetLine> findByProjectIdAndCountryIdAndCategoryAndMonth(Long projectId, Long countryId, String category, String month);
    void deleteByProjectId(Long projectId);
}
