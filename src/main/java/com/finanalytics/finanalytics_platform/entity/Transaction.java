package com.finanalytics.finanalytics_platform.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Logs the actual financial events or movements of money, which serve as the raw data needed to look for suspicious patterns. 
// Each transaction captures comprehensive metadata to support complex fraud rules.
@Entity
@Table(name = "transactions", indexes = {
        @Index(name = "idx_tx_sender_id",   columnList = "sender_id"),
        @Index(name = "idx_tx_created_at",  columnList = "created_at"),
        @Index(name = "idx_tx_flagged",     columnList = "is_flagged"),
        @Index(name = "idx_tx_status",      columnList = "status")
})
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // UUID exposed to clients — never expose the numeric PK
    @Column(name = "reference_id", nullable = false, unique = true, length = 36)
    private String referenceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Column(nullable = false, precision = 15, scale = 2)
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    // ISO-4217 e.g. ZAR, USD
    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod paymentMethod;

    // Supports IPv6 (45 chars max)
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "device_id", length = 128)
    private String deviceId;

    // ISO-3166 alpha-2 e.g. ZA, US
    @Column(name = "location_country", length = 2)
    private String locationCountry;

    @Column(name = "location_city", length = 100)
    private String locationCity;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "merchant_name", length = 200)
    private String merchantName;

    @Column(name = "merchant_category", length = 100)
    private String merchantCategory;

    // Risk score 0-100 from fraud engine
    @Column(name = "risk_score")
    private Integer riskScore;

    @Column(name = "is_flagged")
    private Boolean isFlagged = false;

    @Column(name = "flag_reason", columnDefinition = "TEXT")
    private String flagReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "fraud_review_status")
    private FraudReviewStatus fraudReviewStatus = FraudReviewStatus.PENDING;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    // Additional fields can be added as needed to support more complex rules, e.g. card BIN, wallet provider, etc.

    public enum TransactionStatus {
        PENDING, COMPLETED, FAILED, BLOCKED, REFUNDED
    }

    public enum PaymentMethod {
        CARD, EFT, INSTANT_EFT, WALLET, CRYPTO
    }

    public enum FraudReviewStatus {
        PENDING, UNDER_REVIEW, CLEARED, CONFIRMED_FRAUD
    }
}