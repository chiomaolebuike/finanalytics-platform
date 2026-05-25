package com.finanalytics.finanalytics_platform.dto;

import com.finanalytics.finanalytics_platform.entity.Transaction;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record TransactionRequest(
        @NotNull                                            Long receiverId,
        @NotNull @DecimalMin("0.01")                        BigDecimal amount,
        @Size(min = 3, max = 3)                             String currency,
        @NotNull                                            Transaction.PaymentMethod paymentMethod,
        @Size(max = 128)                                    String deviceId,
        @Size(min = 2, max = 2)                             String locationCountry,
        @Size(max = 100)                                    String locationCity,
        @DecimalMin("-90")  @DecimalMax("90")               Double latitude,
        @DecimalMin("-180") @DecimalMax("180")              Double longitude,
        @Size(max = 200)                                    String merchantName,
        @Size(max = 100)                                    String merchantCategory
) {}