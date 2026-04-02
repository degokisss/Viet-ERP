package com.vieterp.otb.planning.dto;

import lombok.*;
import java.math.BigDecimal;

@Builder
public record PlanningCategoryDto(
    String subcategoryId,
    BigDecimal actualBuyPct,
    BigDecimal actualSalesPct,
    BigDecimal actualStPct,
    BigDecimal proposedBuyPct,
    BigDecimal otbProposedAmount,
    BigDecimal varLastyearPct,
    BigDecimal otbActualAmount,
    BigDecimal otbActualBuyPct
) {}
