package com.finanalytics.finanalytics_platform.dto;

public record FraudResponse(
        int    riskScore,   // 0-100
        String riskLevel    // LOW / MEDIUM / HIGH
) {}