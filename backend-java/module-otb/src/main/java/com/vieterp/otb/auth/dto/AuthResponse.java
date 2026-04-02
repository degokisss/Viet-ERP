package com.vieterp.otb.auth.dto;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    UserDto user
) {
    public record UserDto(
        Long id,
        String email,
        String name
    ) {}
}
