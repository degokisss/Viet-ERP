package com.vieterp.otb.ai.dto;

import lombok.*;
import java.math.BigDecimal;

@Data
public class GenerateSkuRecommendationsDto {
    private Long subCategoryId;
    private Long brandId;
    private BigDecimal budgetAmount;
    private Integer maxResults;
}
