package com.segula.saasgestion.repository;
import com.segula.saasgestion.domain.Industry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface IndustryRepository extends JpaRepository<Industry, Long> {
    List<Industry> findAllByIsActiveTrueOrderByNameAsc();
}