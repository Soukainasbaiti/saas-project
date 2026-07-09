package com.segula.saasgestion.repository;

import com.segula.saasgestion.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByEmailIgnoreCaseAndDeletedAtIsNull(String email);

    // La contrainte d'unicite email en base porte sur TOUS les utilisateurs
    // (meme desactives/supprimes), il faut donc verifier sans le filtre deletedAt IS NULL.
    boolean existsByEmailIgnoreCase(String email);
    Optional<AppUser> findByEmailIgnoreCase(String email);

    @Query("SELECT u FROM AppUser u WHERE u.role IN ('PM','BUM') AND u.isActive = true AND u.deletedAt IS NULL ORDER BY u.fullName ASC")
    List<AppUser> findAllActivePMs();

    // ── Nouveau : trouver tous les admins actifs pour envoi email ─
    @Query("SELECT u FROM AppUser u WHERE u.role = 'ADMIN' AND u.isActive = true AND u.deletedAt IS NULL")
    List<AppUser> findAllActiveAdmins();
}