package com.segula.saasgestion.repository;

import com.segula.saasgestion.domain.WipMonthDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface WipMonthDocumentRepository extends JpaRepository<WipMonthDocument, Long> {

    List<WipMonthDocument> findByProjectIdAndYearAndMonth(Long projectId, Integer year, Integer month);

    Optional<WipMonthDocument> findByProjectIdAndYearAndMonthAndDocumentType(
            Long projectId, Integer year, Integer month, String documentType);

    @Query("SELECT d FROM WipMonthDocument d WHERE d.projectId = :projectId ORDER BY d.year, d.month")
    List<WipMonthDocument> findByProjectIdOrderByDate(@Param("projectId") Long projectId);
}
