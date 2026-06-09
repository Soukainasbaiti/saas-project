package com.segula.saasgestion.repository;

import com.segula.saasgestion.domain.Project;
import com.segula.saasgestion.domain.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("SELECT p FROM Project p " +
           "JOIN FETCH p.frontFinancier " +
           "JOIN FETCH p.bu " +
           "JOIN FETCH p.customer " +
           "WHERE p.deletedAt IS NULL " +
           "AND (:buId IS NULL OR :buId = '' OR p.bu.id = :buId) " +
           "AND (:customerId IS NULL OR p.customer.id = :customerId) " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND (:createdById IS NULL OR p.createdById = :createdById)")
    Page<Project> findWithFiltersNoYear(
        @Param("buId")        String buId,
        @Param("customerId")  Long   customerId,
        @Param("status")      ProjectStatus status,
        @Param("createdById") Long   createdById,
        Pageable pageable
    );

    @Query("SELECT p FROM Project p " +
           "JOIN FETCH p.frontFinancier " +
           "JOIN FETCH p.bu " +
           "JOIN FETCH p.customer " +
           "WHERE p.deletedAt IS NULL " +
           "AND (:buId IS NULL OR :buId = '' OR p.bu.id = :buId) " +
           "AND (:customerId IS NULL OR p.customer.id = :customerId) " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND (:createdById IS NULL OR p.createdById = :createdById) " +
           "AND p.projectYear = :year")
    Page<Project> findWithFiltersWithYear(
        @Param("buId")        String buId,
        @Param("customerId")  Long   customerId,
        @Param("status")      ProjectStatus status,
        @Param("createdById") Long   createdById,
        @Param("year")        Short  year,
        Pageable pageable
    );

    @Query("SELECT p FROM Project p " +
           "JOIN FETCH p.frontFinancier " +
           "JOIN FETCH p.projectManager " +
           "JOIN FETCH p.bu " +
           "JOIN FETCH p.customer " +
           "JOIN FETCH p.industry " +
           "JOIN FETCH p.engineeringDiscipline " +
           "LEFT JOIN FETCH p.function " +
           "JOIN FETCH p.engagement " +
           "WHERE p.id = :id")
    Optional<Project> findByIdWithAllRelations(@Param("id") Long id);

    @Query("SELECT p.id, COALESCE(p.projectName, p.activity), " +
           "cfg.validatedBy, cfg.rejectionComment, cfg.validatedAt " +
           "FROM Project p " +
           "JOIN ProjectManagementConfig cfg ON cfg.projectId = p.id " +
           "WHERE p.deletedAt IS NULL " +
           "AND p.createdById = :userId " +
           "AND cfg.validationStatus = 'REJECTED'")
    List<Object[]> findRejectedByPmId(@Param("userId") Long userId);
}