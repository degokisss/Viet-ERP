package com.vieterp.otb.planning.dto;

import lombok.*;
import java.util.List;

@Builder
public record PlanningFilterOptionsResponse(
    List<GenderOption> genders,
    List<CategoryOption> categories,
    List<SubCategoryOption> subCategories
) {
    @Builder
    public record GenderOption(
        Long id,
        String name
    ) {}

    @Builder
    public record CategoryOption(
        Long id,
        String name,
        Long genderId
    ) {}

    @Builder
    public record SubCategoryOption(
        Long id,
        String name,
        Long categoryId
    ) {}
}
