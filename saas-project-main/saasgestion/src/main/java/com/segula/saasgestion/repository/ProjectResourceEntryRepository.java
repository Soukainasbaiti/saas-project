package com.segula.saasgestion.repository;

import com.segula.saasgestion.domain.ProjectResourceEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProjectResourceEntryRepository extends JpaRepository<ProjectResourceEntry, Long> {
    List<ProjectResourceEntry> findByResourceIdIn(List<Long> resourceIds);
    Optional<ProjectResourceEntry> findByResourceIdAndMonth(Long resourceId, String month);
}
