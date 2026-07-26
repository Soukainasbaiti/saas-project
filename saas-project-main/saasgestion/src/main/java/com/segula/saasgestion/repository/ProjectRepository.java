package com.segula.saasgestion.repository;

import com.segula.saasgestion.domain.Project;
import com.segula.saasgestion.domain.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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
           "AND (:createdById IS NULL OR p.createdById = :createdById " +
           "     OR EXISTS (SELECT 1 FROM ProjectCountry pc WHERE pc.project = p AND pc.pm.id = :createdById))")
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
           "AND (:createdById IS NULL OR p.createdById = :createdById " +
           "     OR EXISTS (SELECT 1 FROM ProjectCountry pc WHERE pc.project = p AND pc.pm.id = :createdById)) " +
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

    // Mise à jour ciblée (TCV/Budget par pays) : évite un save() complet de l'entité,
    // qui réécrirait aussi les colonnes enum (status, technical_office) sans raison.
    // margin_budget/project_margin sont normalement recalculées par le trigger Postgres
    // trg_project_calc_margin, mais on les recalcule aussi ici explicitement (même formule)
    // pour garantir la cohérence même si le trigger ne se déclenche pas sur un UPDATE en masse.
    @Modifying
    @Query(value = "UPDATE project SET revenue_budget = :revenue, cost_budget = :cost, " +
                   "margin_budget = :revenue - :cost, " +
                   "project_margin = CASE WHEN :revenue = 0 THEN 0 ELSE (:revenue - :cost) / :revenue END " +
                   "WHERE id = :id", nativeQuery = true)
    void updateBudgetTotals(@Param("id") Long id, @Param("revenue") BigDecimal revenue, @Param("cost") BigDecimal cost);
}