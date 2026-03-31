package com.segula.saasgestion.repository;
import com.segula.saasgestion.domain.Engagement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EngagementRepository extends JpaRepository<Engagement, Long> {
    List<Engagement> findAllByIsActiveTrueOrderByNameAsc();
}