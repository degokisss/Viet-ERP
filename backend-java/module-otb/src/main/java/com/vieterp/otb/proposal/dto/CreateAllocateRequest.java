package com.vieterp.otb.proposal.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Builder
public record CreateAllocateRequest(
    @NotEmpty(message = "allocations required")
    List<SKUAllocateDto> allocations
) {}
