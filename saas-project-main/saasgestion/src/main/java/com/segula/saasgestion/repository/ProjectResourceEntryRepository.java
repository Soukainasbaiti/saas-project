package com.segula.saasgestion.repository;

import com.segula.saasgestion.domain.ProjectResourceEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProjectResourceEntryRepository extends JpaRepository<ProjectResourceEntry, Long> {
    List<ProjectResourceEntry> findByResourceIdIn(List<Long> resourceIds);
    Optional<ProjectResourceEntry> findByResourceIdAndMonth(Long resourceId, String month);

    @Query("SELECT COALESCE(SUM(e.billedDays), 0) FROM ProjectResourceEntry e " +
           "WHERE e.resource.id IN " +
           "(SELECT r.id FROM ProjectResource r WHERE r.project.id = :projectId) " +
           "AND e.month = :period")
    BigDecimal sumBilledDaysByProjectAndPeriod(@Param("projectId") Long projectId, @Param("period") String period);

    @Query("SELECT COALESCE(SUM(e.billedDays * e.dailyRate), 0) FROM ProjectResourceEntry e " +
           "WHERE e.resource.id IN " +
           "(SELECT r.id FROM ProjectResource r WHERE r.project.id = :projectId) " +
           "AND e.month = :period")
    BigDecimal sumDeclaredAmountByProjectAndPeriod(@Param("projectId") Long projectId, @Param("period") String period);
}
