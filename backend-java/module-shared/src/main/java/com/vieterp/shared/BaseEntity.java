package com.vieterp.shared;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

/**
 * Base class for all JPA entities with common audit fields.
 */
@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEntity {

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}