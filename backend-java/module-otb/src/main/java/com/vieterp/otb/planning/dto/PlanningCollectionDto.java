package com.vieterp.otb.planning.dto;

import lombok.*;
import java.math.BigDecimal;

@Builder
public record PlanningCollectionDto(
    String seasonTypeId,
    String storeId,
    BigDecimal actualBuyPct,
    BigDecimal actualSalesPct,
    BigDecimal actualStPct,
    BigDecimal actualMoc,
    BigDecimal proposedBuyPct,
    BigDecimal otbProposedAmount,
    BigDecimal pctVarVsLast
) {}
