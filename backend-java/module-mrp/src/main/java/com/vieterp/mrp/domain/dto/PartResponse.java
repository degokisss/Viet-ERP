package com.vieterp.mrp.domain.dto;

import com.vieterp.mrp.domain.enums.MakeOrBuy;
import com.vieterp.mrp.domain.enums.LifecycleStatus;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Builder
public record PartResponse(
    UUID id,
    String partNumber,
    String name,
    String description,
    MakeOrBuy makeOrBuy,
    LifecycleStatus lifecycleStatus,
    String unitOfMeasure,
    Integer shelfLifeDays,
    BigDecimal weight,
    BigDecimal volume,
    BigDecimal minStockLevel,
    BigDecimal maxStockLevel,
    BigDecimal reorderPoint,
    Integer leadTimeDays,
    Boolean isActive,
    String tenantId,
    String createdBy,
    Instant createdAt,
    Instant updatedAt
) {}
