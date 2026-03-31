package com.segula.saasgestion.repository;

import com.segula.saasgestion.domain.BU;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface BuRepository extends JpaRepository<BU, String> {
    List<BU> findAllByIsActiveTrueOrderByNameAsc();
    Optional<BU> findByTrigram(String trigram);
}