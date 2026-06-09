package com.segula.saasgestion.repository;

import com.segula.saasgestion.domain.ProjectMip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ProjectMipRepository extends JpaRepository<ProjectMip, Long> {

    @Query("SELECT m FROM ProjectMip m WHERE m.projectId = :projectId ORDER BY m.mipId ASC")
    List<ProjectMip> findByProjectId(@Param("projectId") Long projectId);

    int countByProjectId(Long projectId);
}
