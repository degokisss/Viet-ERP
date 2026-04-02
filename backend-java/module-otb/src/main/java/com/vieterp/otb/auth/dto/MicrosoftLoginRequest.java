package com.vieterp.otb.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
public class MicrosoftLoginRequest {
    @NotBlank(message = "Access token is required")
    private String accessToken;
}
