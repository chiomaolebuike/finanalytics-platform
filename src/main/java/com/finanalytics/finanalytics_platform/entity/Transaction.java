package com.finanalytics.finanalytics_platform.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
//@Table(name = "transactions")
public class Transaction {
    private Long id;

    private String referenecId;

    private User user;

    private Product product;

    private Double amount;

    private TransactionStatus status; // PENDING, COMPLETED, FAILED, REFUNDED

    private PaymentMethod paymentMethod; // CARD, WALLET, EFT, INSTANT_EFT
    
    private String merchantName;
    private String category; // e.g., "Groceries", "Utilities", "Entertainment"

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public enum TransactionStatus { PENDING, COMPLETED, FAILED, REFUNDED }
    public enum PaymentMethod     { CARD, WALLET, EFT, INSTANT_EFT }
}
