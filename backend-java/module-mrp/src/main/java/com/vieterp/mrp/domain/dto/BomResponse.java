package com.vieterp.mrp.domain.dto;

import com.vieterp.mrp.domain.enums.BomType;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Builder
public record BomResponse(
    UUID id,
    String bomNumber,
    String name,
    BomType bomType,
    String partId,
    BigDecimal quantity,
    Boolean isActive,
    LocalDate effectiveFrom,
    LocalDate effectiveTo,
    String tenantId,
    String createdBy,
    Instant createdAt,
    Instant updatedAt
) {}
