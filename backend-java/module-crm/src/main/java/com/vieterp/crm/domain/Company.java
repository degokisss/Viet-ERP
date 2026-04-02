package com.vieterp.crm.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "companies")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 255)
    private String domain;

    @Column(length = 100)
    private String industry;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CompanySize size;

    @Column(length = 50)
    private String phone;

    @Column(length = 191)
    private String email;

    @Column(length = 255)
    private String website;

    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String province;

    @Column(length = 10)
    private String country = "VN";

    @Column(length = 50)
    private String taxCode;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "owner_id", nullable = false, length = 100)
    private String ownerId;

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public enum CompanySize {
        SOLO, SMALL, MEDIUM, LARGE, ENTERPRISE
    }
}
