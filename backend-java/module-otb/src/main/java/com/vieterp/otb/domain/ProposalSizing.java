package com.vieterp.otb.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "proposal_sizing")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProposalSizing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "proposal_sizing_header_id")
    private Long proposalSizingHeaderId;

    @Column(name = "sku_proposal_id")
    private Long skuProposalId;

    @Column(name = "subcategory_size_id")
    private Long subcategorySizeId;

    @Column(name = "actual_salesmix_pct")
    private BigDecimal actualSalesmixPct;

    @Column(name = "actual_st_pct")
    private BigDecimal actualStPct;

    @Column(name = "proposal_quantity")
    private Integer proposalQuantity;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at", nullable = true)
    private Instant updatedAt;
}
