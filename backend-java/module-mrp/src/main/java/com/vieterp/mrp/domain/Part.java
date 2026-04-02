package com.vieterp.mrp.domain;

import com.vieterp.mrp.domain.enums.MakeOrBuy;
import com.vieterp.mrp.domain.enums.LifecycleStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "mrp_parts")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Part {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "part_number", nullable = false, length = 100)
    private String partNumber;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "make_or_buy", nullable = false, length = 20)
    private MakeOrBuy makeOrBuy;

    @Enumerated(EnumType.STRING)
    @Column(name = "lifecycle_status", nullable = false, length = 20)
    private LifecycleStatus lifecycleStatus;

    @Column(name = "unit_of_measure", nullable = false, length = 20)
    private String unitOfMeasure;

    @Column(name = "shelf_life_days")
    private Integer shelfLifeDays;

    @Column(precision = 18, scale = 4)
    private BigDecimal weight;

    @Column(precision = 18, scale = 4)
    private BigDecimal volume;

    @Column(name = "min_stock_level", precision = 18, scale = 4)
    private BigDecimal minStockLevel;

    @Column(name = "max_stock_level", precision = 18, scale = 4)
    private BigDecimal maxStockLevel;

    @Column(name = "reorder_point", precision = 18, scale = 4)
    private BigDecimal reorderPoint;

    @Column(name = "lead_time_days")
    private Integer leadTimeDays;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

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
