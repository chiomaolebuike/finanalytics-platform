package com.finanalytics.finanalytics_platform.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String error
) {}
