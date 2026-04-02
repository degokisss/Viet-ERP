package com.vieterp.otb.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "planning_category")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlanningCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "subcategory_id")
    private Long subcategoryId;

    @Column(name = "planning_header_id")
    private Long planningHeaderId;

    @Column(name = "actual_buy_pct")
    private BigDecimal actualBuyPct;

    @Column(name = "actual_sales_pct")
    private BigDecimal actualSalesPct;

    @Column(name = "actual_st_pct")
    private BigDecimal actualStPct;

    @Column(name = "proposed_buy_pct")
    private BigDecimal proposedBuyPct;

    @Column(name = "otb_proposed_amount")
    private BigDecimal otbProposedAmount;

    @Column(name = "var_lastyear_pct")
    private BigDecimal varLastyearPct;

    @Column(name = "otb_actual_amount")
    private BigDecimal otbActualAmount;

    @Column(name = "otb_actual_buy_pct")
    private BigDecimal otbActualBuyPct;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "updated_at", nullable = true)
    private Instant updatedAt;
}
