package com.vieterp.crm.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contacts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(length = 191)
    private String email;

    @Column(length = 50)
    private String phone;

    @Column(length = 50)
    private String mobile;

    @Column(name = "job_title", length = 100)
    private String jobTitle;

    @Column(length = 100)
    private String department;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private LeadSource source;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ContactStatus status = ContactStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(length = 10)
    private String country;

    @Column(name = "external_hrm_id", length = 100)
    private String externalHrmId;

    @Column(nullable = false)
    private Integer score = 0;

    @Column(name = "last_activity_at")
    private Instant lastActivityAt;

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

    public enum LeadSource {
        WEBSITE, REFERRAL, COLD_CALL, EMAIL, SOCIAL_MEDIA, TRADE_SHOW, ADVERTISEMENT, PARTNER, OTHER
    }

    public enum ContactStatus {
        ACTIVE, INACTIVE, LEAD, CUSTOMER, CHURNED
    }
}
