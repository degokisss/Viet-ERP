package com.vieterp.otb.planning.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.util.List;

@Builder
public record CreatePlanningRequest(
    @NotBlank(message = "allocateHeaderId is required")
    String allocateHeaderId,

    List<PlanningCollectionDto> seasonTypes,

    List<PlanningGenderDto> genders,

    List<PlanningCategoryDto> categories
) {}
