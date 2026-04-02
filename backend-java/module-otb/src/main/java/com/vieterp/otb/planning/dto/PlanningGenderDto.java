package com.vieterp.otb.planning.dto;

import lombok.*;
import java.math.BigDecimal;

@Builder
public record PlanningGenderDto(
    String genderId,
    String storeId,
    BigDecimal actualBuyPct,
    BigDecimal actualSalesPct,
    BigDecimal actualStPct,
    BigDecimal proposedBuyPct,
    BigDecimal otbProposedAmount,
    BigDecimal pctVarVsLast
) {}
