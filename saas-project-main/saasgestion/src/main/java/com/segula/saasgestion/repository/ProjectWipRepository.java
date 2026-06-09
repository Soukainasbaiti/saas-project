package com.segula.saasgestion.repository;

import com.segula.saasgestion.domain.ProjectWip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;

public interface ProjectWipRepository extends JpaRepository<ProjectWip, Long> {

    @Query("SELECT w FROM ProjectWip w WHERE w.projectId = :projectId ORDER BY w.period DESC")
    List<ProjectWip> findByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT w FROM ProjectWip w WHERE w.projectId = :projectId AND w.period = :period")
    List<ProjectWip> findByProjectIdAndPeriod(@Param("projectId") Long projectId, @Param("period") String period);

    int countByProjectId(Long projectId);

    @Query("SELECT COALESCE(SUM(w.wipAmount), 0) FROM ProjectWip w WHERE w.projectId = :projectId AND w.status <> 'CLOSED'")
    BigDecimal sumOpenWipAmountByProjectId(@Param("projectId") Long projectId);

    @Query("SELECT COALESCE(SUM(w.wipAmount), 0) FROM ProjectWip w WHERE w.status <> 'CLOSED'")
    BigDecimal sumAllOpenWipAmount();
}
