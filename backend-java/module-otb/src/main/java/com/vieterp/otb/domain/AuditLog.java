package com.vieterp.otb.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "audit_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(length = 191)
    private String action;

    @Column(name = "entity_type", length = 191)
    private String entityType;

    @Column(name = "entity_id", length = 191)
    private String entityId;

    @Column(length = 191)
    private String changes;

    @Column(name = "ip_address", length = 191)
    private String ipAddress;

    @Column(name = "created_at")
    private Instant createdAt;
}
