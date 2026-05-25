package com.finanalytics.finanalytics_platform.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Stores rolling 30-day statistical fingerprint per user.
 * Updated asynchronously after each transaction — never blocks the payment path.
 * Powers Rule 6 (amount anomaly) and Rule 4 (unknown device) in the fraud engine.
 */
@Entity
@Table(name = "user_behaviour_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBehaviourProfile {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    // Rolling 30-day mean transaction amount
    @Column(name = "avg_tx_amount_30d", precision = 15, scale = 2)
    private BigDecimal avgTxAmount30d = BigDecimal.ZERO;

    // Standard deviation — used for z-score anomaly detection
    @Column(name = "std_dev_amount_30d", precision = 15, scale = 2)
    private BigDecimal stdDevAmount30d = BigDecimal.ZERO;

    // Average number of transactions per day over 30 days
    @Column(name = "avg_daily_tx_count_30d")
    private Double avgDailyTxCount30d = 0.0;

    // Maximum single transaction in last 30 days
    @Column(name = "max_single_tx_30d", precision = 15, scale = 2)
    private BigDecimal maxSingleTx30d = BigDecimal.ZERO;

    // Comma-separated ISO-3166 country codes seen for this user
    @Column(name = "usual_countries", length = 500)
    private String usualCountries;

    // JSON array of known device IDs
    @Column(name = "known_device_ids", columnDefinition = "TEXT")
    private String knownDeviceIds;

    @Column(name = "total_tx_count")
    private Long totalTxCount = 0L;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}