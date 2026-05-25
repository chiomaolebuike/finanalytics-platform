package com.finanalytics.finanalytics_platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ProductDto(
        @NotBlank                   String name,
        String                      description,
        @Positive                   BigDecimal price,
        @PositiveOrZero             Integer stock,
        String                      imageUrl,
        String                      category
) {}