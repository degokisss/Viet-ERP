package com.vieterp.accounting.domain;

import com.vieterp.accounting.domain.enums.AccountGroup;
import com.vieterp.accounting.domain.enums.AccountType;
import com.vieterp.accounting.domain.enums.NormalBalance;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "acc_accounts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_number", nullable = false, length = 50)
    private String accountNumber;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(name = "name_en", length = 255)
    private String nameEn;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_group", nullable = false, length = 20)
    private AccountGroup accountGroup;

    @Enumerated(EnumType.STRING)
    @Column(name = "normal_balance", nullable = false, length = 10)
    private NormalBalance normalBalance;

    @Column(name = "parent_id", length = 100)
    private String parentId;

    @Column(name = "level", nullable = false)
    private Integer level = 1;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_system_account", nullable = false)
    private Boolean isSystemAccount = false;

    @Column(name = "is_bank_account", nullable = false)
    private Boolean isBankAccount = false;

    @Column(length = 10)
    private String currency = "VND";

    @Column(name = "vas_code", length = 50)
    private String vasCode;

    @Column(name = "ifrs_code", length = 50)
    private String ifrsCode;

    @Column(name = "tt200_code", length = 50)
    private String tt200Code;

    @Column(name = "tt133_code", length = 50)
    private String tt133Code;

    @Column(name = "opening_balance", nullable = false, precision = 18, scale = 4)
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(name = "current_balance", nullable = false, precision = 18, scale = 4)
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Column(name = "tenant_id", nullable = false, length = 100)
    private String tenantId;

    @Column(name = "created_by", length = 100)
    private String createdBy;

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
}
