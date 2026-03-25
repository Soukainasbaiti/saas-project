package com.segula.saasgestion.repository;

import com.segula.saasgestion.domain.ProjectFunction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectFunctionRepository extends JpaRepository<ProjectFunction, Long> {
    List<ProjectFunction> findAllByIsActiveTrueOrderByNameAsc();

    @Query("SELECT f FROM ProjectFunction f WHERE f.isActive = true AND (f.engineeringDiscipline IS NULL OR f.engineeringDiscipline.id = :disciplineId) ORDER BY f.name ASC")
    List<ProjectFunction> findByDisciplineOrGlobal(@Param("disciplineId") Long disciplineId);

    Optional<ProjectFunction> findByNameIgnoreCase(String name);
}