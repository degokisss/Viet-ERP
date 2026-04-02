package com.vieterp.otb.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sku_code", length = 191)
    private String skuCode;

    @Column(name = "product_name", length = 191)
    private String productName;

    @Column(name = "sub_category_id")
    private Long subCategoryId;

    @Column(name = "brand_id")
    private Long brandId;

    @Column(length = 191)
    private String family;

    @Column(length = 191)
    private String theme;

    @Column(length = 191)
    private String color;

    @Column(length = 191)
    private String composition;

    private BigDecimal srp;

    @Column(name = "image_url", length = 191)
    private String imageUrl;

    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at", nullable = true)
    private Instant updatedAt;
}
