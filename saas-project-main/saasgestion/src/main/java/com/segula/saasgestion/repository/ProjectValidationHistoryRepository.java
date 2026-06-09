package com.segula.saasgestion.repository;

import com.segula.saasgestion.domain.ProjectValidationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ProjectValidationHistoryRepository extends JpaRepository<ProjectValidationHistory, Long> {

    @Query("""
        SELECT h FROM ProjectValidationHistory h
        ORDER BY h.createdAt DESC
        """)
    List<ProjectValidationHistory> findAllOrderByDateDesc();

    List<ProjectValidationHistory> findByProjectIdOrderByCreatedAtDesc(Long projectId);
}
