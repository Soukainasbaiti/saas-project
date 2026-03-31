// ─── Bu.java ────────────────────────────────────────────────────────────────
package com.segula.saasgestion.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.CreationTimestamp;
import java.time.OffsetDateTime;

@Entity @Table(name = "bu")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class BU {
    @Id
    @Column(name = "id", length = 10, nullable = false)
    private String id;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "trigram", length = 5, nullable = false)
    private String trigram;

    @Column(name = "bum_name", length = 150, nullable = false)
    private String bumName;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}