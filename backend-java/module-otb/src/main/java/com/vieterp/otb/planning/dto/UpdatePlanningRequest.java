package com.vieterp.otb.planning.dto;

import lombok.*;
import java.util.List;

@Builder
public record UpdatePlanningRequest(
    String allocateHeaderId,

    List<PlanningCollectionDto> seasonTypes,

    List<PlanningGenderDto> genders,

    List<PlanningCategoryDto> categories
) {}
