package com.segula.saasgestion.repository;
import com.segula.saasgestion.domain.EngineeringDiscipline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EngineeringDisciplineRepository extends JpaRepository<EngineeringDiscipline, Long> {
    List<EngineeringDiscipline> findAllByIsActiveTrueOrderByNameAsc();
}