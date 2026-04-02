package com.vieterp.otb.ai.dto;

import java.util.List;

public record CompareSizeCurveResponse(
    String alignment,
    Integer score,
    List<SizeDeviation> deviations,
    String suggestion
) {
    public record SizeDeviation(
        String sizeName,
        Double expectedPct,
        Double actualPct,
        Double deviation,
        String severity
    ) {}
}
