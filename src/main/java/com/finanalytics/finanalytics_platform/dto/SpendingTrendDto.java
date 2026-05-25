package com.finanalytics.finanalytics_platform.dto;

import java.math.BigDecimal;

public record SpendingTrendDto(
        String     month,
        BigDecimal totalAmount
) {}