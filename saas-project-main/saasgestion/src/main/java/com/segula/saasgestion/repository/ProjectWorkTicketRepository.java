package com.segula.saasgestion.repository;

import com.segula.saasgestion.domain.ProjectWorkTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProjectWorkTicketRepository extends JpaRepository<ProjectWorkTicket, Long> {
    List<ProjectWorkTicket> findByProjectIdOrderByTicketIdAsc(Long projectId);
    List<ProjectWorkTicket> findByProjectIdAndWorkTypeIdOrderByTicketIdAsc(Long projectId, Long workTypeId);
    int countByProjectId(Long projectId);
}
