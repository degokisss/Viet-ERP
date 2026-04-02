package com.vieterp.mrp.domain.dto;

import com.vieterp.mrp.domain.enums.MakeOrBuy;
import com.vieterp.mrp.domain.enums.LifecycleStatus;
import jakarta.validation.constraints.*;
import lombok.Builder;
import java.math.BigDecimal;

@Builder
public record CreatePartRequest(
    @NotBlank(message = "Part number is required") @Size(max = 100) String partNumber,
    @NotBlank(message = "Name is required") @Size(max = 255) String name,
    String description,
    @NotNull(message = "Make or Buy is required") MakeOrBuy makeOrBuy,
    @NotNull(message = "Lifecycle status is required") LifecycleStatus lifecycleStatus,
    @NotBlank(message = "Unit of measure is required") @Size(max = 20) String unitOfMeasure,
    @Min(value = 0, message = "Shelf life days must be non-negative") Integer shelfLifeDays,
    @DecimalMin(value = "0.0", message = "Weight must be non-negative") BigDecimal weight,
    @DecimalMin(value = "0.0", message = "Volume must be non-negative") BigDecimal volume,
    @DecimalMin(value = "0.0", message = "Min stock level must be non-negative") BigDecimal minStockLevel,
    @DecimalMin(value = "0.0", message = "Max stock level must be non-negative") BigDecimal maxStockLevel,
    @DecimalMin(value = "0.0", message = "Reorder point must be non-negative") BigDecimal reorderPoint,
    @Min(value = 0, message = "Lead time days must be non-negative") Integer leadTimeDays,
    Boolean isActive,
    @NotBlank(message = "Tenant ID is required") String tenantId,
    String createdBy
) {}
