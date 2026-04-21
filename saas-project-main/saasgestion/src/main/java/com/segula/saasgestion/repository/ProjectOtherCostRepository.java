package com.segula.saasgestion.repository;

import com.segula.saasgestion.domain.ProjectOtherCost;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProjectOtherCostRepository extends JpaRepository<ProjectOtherCost, Long> {
    List<ProjectOtherCost> findByProjectIdOrderByCategoryAscMonthAsc(Long projectId);
    Optional<ProjectOtherCost> findByProjectIdAndCategoryAndMonth(Long projectId, String category, String month);
    List<ProjectOtherCost> findByProjectIdAndCategory(Long projectId, String category);
}
