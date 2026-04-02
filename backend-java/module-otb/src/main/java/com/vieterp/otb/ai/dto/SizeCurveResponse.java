package com.vieterp.otb.ai.dto;

public record SizeCurveResponse(
    String sizeName,
    Double recommendedPct,
    Integer recommendedQty,
    Double confidence,
    String reasoning
) {}
