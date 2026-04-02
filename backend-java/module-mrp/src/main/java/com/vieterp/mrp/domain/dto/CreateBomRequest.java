package com.vieterp.mrp.domain.dto;

import com.vieterp.mrp.domain.enums.BomType;
import jakarta.validation.constraints.*;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record CreateBomRequest(
    @NotBlank(message = "BOM number is required") @Size(max = 100) String bomNumber,
    @NotBlank(message = "Name is required") @Size(max = 255) String name,
    @NotNull(message = "BOM type is required") BomType bomType,
    @NotBlank(message = "Part ID is required") String partId,
    @NotNull(message = "Quantity is required") @DecimalMin(value = "0.0", message = "Quantity must be non-negative") BigDecimal quantity,
    Boolean isActive,
    LocalDate effectiveFrom,
    LocalDate effectiveTo,
    @NotBlank(message = "Tenant ID is required") String tenantId,
    String createdBy
) {}
