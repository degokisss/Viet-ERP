package com.vieterp.otb.imports.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportDeleteDto {
    private ImportTargetEnum target;
    private String sessionId;
    private Boolean clearAll;
}
