package com.segula.saasgestion.repository;

import com.segula.saasgestion.domain.FrontFinancier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface FrontFinancierRepository extends JpaRepository<FrontFinancier, Long> {
    List<FrontFinancier> findAllByIsActiveTrueOrderByCodeAsc();
    Optional<FrontFinancier> findByCodeIgnoreCase(String code);
}