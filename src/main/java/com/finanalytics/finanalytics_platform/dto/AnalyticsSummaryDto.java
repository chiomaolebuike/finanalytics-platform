package com.finanalytics.finanalytics_platform.dto;

import java.math.BigDecimal;
import java.util.Map;

public record AnalyticsSummaryDto(
        BigDecimal              totalSpend,
        BigDecimal              averageOrderValue,
        double                  failureRate,
        Map<String, BigDecimal> categoryBreakdown
) {}