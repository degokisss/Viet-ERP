package com.vieterp.otb.proposal.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
public class FullSizingHeaderDto {
    private Boolean isFinalVersion;
    private List<FullSizingDto> sizings;
}
