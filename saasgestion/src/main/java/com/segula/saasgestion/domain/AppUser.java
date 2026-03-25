package com.segula.saasgestion.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.OffsetDateTime;

@Entity @Table(name = "app_user")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AppUser {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "full_name", length = 200, nullable = false)
    private String fullName;
    @Column(name = "email", length = 254, nullable = false, unique = true)
    private String email;
    @Column(name = "password_hash", length = 255)
    private String passwordHash;
    @Column(name = "role", length = 50, nullable = false)
    private String role = "USER";
    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
    @CreationTimestamp @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}