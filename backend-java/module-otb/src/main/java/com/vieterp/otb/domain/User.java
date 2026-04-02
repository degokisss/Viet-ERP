package com.vieterp.otb.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "\"user\"")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 191)
    private String email;

    @Column(length = 191)
    private String name;

    @Column(name = "password_hash", length = 191)
    private String passwordHash;

    @Column(name = "role_id")
    private Long roleId;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "store_access", length = 191)
    private String storeAccess;

    @Column(name = "brand_access", length = 191)
    private String brandAccess;

    @Column(name = "updated_at", nullable = true)
    private Instant updatedAt;
}
