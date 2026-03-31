package com.segula.saasgestion.repository;

import com.segula.saasgestion.domain.ProjectPending;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectPendingRepository extends JpaRepository<ProjectPending, Long> {

    Optional<ProjectPending> findByApprovalToken(String approvalToken);

    @Query("SELECT p FROM ProjectPending p WHERE p.status = 'PENDING' ORDER BY p.createdAt DESC")
    List<ProjectPending> findAllPending();
}