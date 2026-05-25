package com.finanalytics.finanalytics_platform.dto;

import java.math.BigDecimal;

public record FraudRequest(
        BigDecimal amount,
        String     locationCountry,
        String     deviceId,
        Double     latitude,
        Double     longitude,
        Long       senderId,
        Integer    recentTxCount,
        Double     distanceFromLastTxKm,
        Boolean    isKnownDevice,
        Integer    transactionHour
) {}