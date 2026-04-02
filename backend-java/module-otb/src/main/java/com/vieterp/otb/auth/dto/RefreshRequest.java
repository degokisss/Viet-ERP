package com.vieterp.otb.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
public class RefreshRequest {
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
