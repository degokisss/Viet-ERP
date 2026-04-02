package com.vieterp.otb.ai.dto;

import lombok.*;

import java.util.Map;

@Data
public class CompareSizeCurveDto {
    private Long subCategoryId;
    private Long storeId;
    private Map<String, Integer> userSizing;
}
