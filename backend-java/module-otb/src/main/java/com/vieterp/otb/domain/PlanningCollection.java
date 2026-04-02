package com.vieterp.otb.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "planning_collection")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanningCollection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "season_type_id")
    private Long seasonTypeId;

    @Column(name = "store_id")
    private Long storeId;

    @Column(name = "planning_header_id")
    private Long planningHeaderId;

    @Column(name = "actual_buy_pct")
    private BigDecimal actualBuyPct;

    @Column(name = "actual_sales_pct")
    private BigDecimal actualSalesPct;

    @Column(name = "actual_st_pct")
    private BigDecimal actualStPct;

    @Column(name = "actual_moc")
    private BigDecimal actualMoc;

    @Column(name = "proposed_buy_pct")
    private BigDecimal proposedBuyPct;

    @Column(name = "otb_proposed_amount")
    private BigDecimal otbProposedAmount;

    @Column(name = "pct_var_vs_last")
    private BigDecimal pctVarVsLast;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at", nullable = true)
    private Instant updatedAt;
}
