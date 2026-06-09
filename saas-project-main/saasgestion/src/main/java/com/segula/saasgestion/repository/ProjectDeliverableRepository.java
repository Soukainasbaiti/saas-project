package com.segula.saasgestion.repository;

import com.segula.saasgestion.domain.ProjectDeliverable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ProjectDeliverableRepository extends JpaRepository<ProjectDeliverable, Long> {

    List<ProjectDeliverable> findByProjectIdOrderByLotNameAscDeliverableIdAsc(Long projectId);

    int countByProjectIdAndLotName(Long projectId, String lotName);

    @Query("SELECT DISTINCT d.lotName FROM ProjectDeliverable d WHERE d.projectId = :projectId ORDER BY d.lotName")
    List<String> findDistinctLotsByProjectId(Long projectId);
}
