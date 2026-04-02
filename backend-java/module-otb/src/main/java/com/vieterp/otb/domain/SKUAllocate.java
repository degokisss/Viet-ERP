package com.vieterp.otb.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "s_k_u_allocate")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SKUAllocate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sku_proposal_id")
    private Long skuProposalId;

    @Column(name = "store_id")
    private Long storeId;

    private BigDecimal quantity;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at", nullable = true)
    private Instant updatedAt;
}
