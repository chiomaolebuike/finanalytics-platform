package com.finanalytics.finanalytics_platform.dto;

public record RiskProfileDto(
        String email,
        String riskLevel,
        int    totalTransactions,
        int    flaggedCount,
        double fraudRate
) {}