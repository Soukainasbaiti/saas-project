package com.segula.saasgestion.repository;
import com.segula.saasgestion.domain.ProjectCountry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectCountryRepository extends JpaRepository<ProjectCountry, Long> {
    List<ProjectCountry> findByProjectIdOrderByDisplayOrderAsc(Long projectId);
    Optional<ProjectCountry> findByProjectIdAndIsLeadTrue(Long projectId);
    List<ProjectCountry> findByProjectIdAndPmId(Long projectId, Long pmId);
    boolean existsByProjectIdAndCountryId(Long projectId, Long countryId);
}
