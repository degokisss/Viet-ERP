package com.vieterp.otb.imports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportBatchDto {
    private ImportTargetEnum target;
    private ImportMode mode;
    private DuplicateHandling duplicateHandling;
    private String[] matchKey;
    private List<Map<String, Object>> rows;
    private Integer batchIndex;
    private Integer totalBatches;
    private String sessionId;

    public ImportMode getMode() {
        return mode != null ? mode : ImportMode.INSERT;
    }

    public DuplicateHandling getDuplicateHandling() {
        return duplicateHandling != null ? duplicateHandling : DuplicateHandling.SKIP;
    }

    public Integer getBatchIndex() {
        return batchIndex != null ? batchIndex : 0;
    }

    public Integer getTotalBatches() {
        return totalBatches != null ? totalBatches : 1;
    }
}
