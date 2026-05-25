package com.finanalytics.finanalytics_platform.dto;

import com.finanalytics.finanalytics_platform.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        String          referenceId,
        BigDecimal      amount,
        String          currency,
        String          status,
        Integer         riskScore,
        Boolean         flagged,
        String          flagReason,
        String          action,
        LocalDateTime   createdAt,
        boolean         blocked
) {
    public static TransactionResponse fromEntity(Transaction t) {
        return new TransactionResponse(
                t.getReferenceId(),
                t.getAmount(),
                t.getCurrency(),
                t.getStatus().name(),
                t.getRiskScore(),
                t.getIsFlagged(),
                t.getFlagReason(),
                null,
                t.getCreatedAt(),
                t.getStatus() == Transaction.TransactionStatus.BLOCKED
        );
    }
}