package com.finanalytics.finanalytics_platform.service;

import com.finanalytics.finanalytics_platform.dto.FraudRequest;
import com.finanalytics.finanalytics_platform.dto.FraudResponse;
import com.finanalytics.finanalytics_platform.entity.Transaction;
import com.finanalytics.finanalytics_platform.entity.UserBehaviourProfile;
import com.finanalytics.finanalytics_platform.repository.TransactionRepository;
import com.finanalytics.finanalytics_platform.repository.UserBehaviourProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * FraudAnalysisService — Rule-based fraud scoring engine.
 *
 * 7 rules, each contributing a weighted score. Total is capped at 100.
 * Score >= 60 → flagged. Score >= 80 → blocked immediately.
 *
 * Rule weights approximate heuristics used by major SA banks (Capitec, FNB, Absa).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FraudAnalysisService {

    private static final BigDecimal HIGH_VALUE_THRESHOLD      = new BigDecimal("10000.00");
    private static final BigDecimal UNUSUAL_AMOUNT_MULTIPLIER = new BigDecimal("3.0");
    private static final int        VELOCITY_WINDOW_MINUTES   = 10;
    private static final int        VELOCITY_TX_LIMIT         = 5;
    private static final double     GEO_ANOMALY_KM            = 500.0;
    private static final int        FLAG_THRESHOLD            = 60;
    private static final int        BLOCK_THRESHOLD           = 80;

    private final TransactionRepository          txRepo;
    private final UserBehaviourProfileRepository profileRepo;

    // ──────────────────────────────────────────────────────────────
    //  MAIN ENTRY POINT
    // ──────────────────────────────────────────────────────────────

    public FraudEvaluation evaluate(Transaction tx) {
        List<RuleResult> results = new ArrayList<>();

        results.add(ruleHighValue(tx));
        results.add(ruleVelocity(tx));
        results.add(ruleGeoAnomaly(tx));
        results.add(ruleUnknownDevice(tx));
        results.add(ruleUnusualHour(tx));
        results.add(ruleAmountAnomaly(tx));
        results.add(ruleNewReceiver(tx));

        int totalScore = Math.min(
                results.stream().mapToInt(RuleResult::score).sum(),
                100
        );

        List<String> triggeredRules = results.stream()
                .filter(r -> r.score() > 0)
                .map(RuleResult::ruleName)
                .toList();

        String flagReason = triggeredRules.isEmpty()
                ? null
                : String.join("; ", triggeredRules);

        Action action = determineAction(totalScore);

        log.info("FRAUD_EVAL txRef={} score={} flagged={} rules= {}",
                tx.getReferenceId(), totalScore, totalScore >= FLAG_THRESHOLD, flagReason);

        return new FraudEvaluation(totalScore, totalScore >= FLAG_THRESHOLD, flagReason, action, results);
    }

    // ──────────────────────────────────────────────────────────────
    //  INDIVIDUAL RULES
    // ──────────────────────────────────────────────────────────────

    /** Rule 1 — High-value transaction (max +30) */
    private RuleResult ruleHighValue(Transaction tx) {
        if (tx.getAmount().compareTo(HIGH_VALUE_THRESHOLD) > 0) {
            int score = tx.getAmount().compareTo(new BigDecimal("50000")) > 0 ? 30 : 20;
            return new RuleResult("HIGH_VALUE_TRANSACTION", score,
                    "Amount " + tx.getAmount() + " exceeds ZAR 10,000");
        }
        return RuleResult.clean("HIGH_VALUE_TRANSACTION");
    }

    /** Rule 2 — Velocity (max +30): 5+ transactions in 10 minutes = card testing */
    private RuleResult ruleVelocity(Transaction tx) {
        LocalDateTime windowStart = LocalDateTime.now().minusMinutes(VELOCITY_WINDOW_MINUTES);
        long recentCount = txRepo.(
                tx.getSender().getId(), windowStart);

        if (recentCount >= VELOCITY_TX_LIMIT) {
            int score = (int) Math.min(30, 10 + (recentCount - VELOCITY_TX_LIMIT) * 5);
            return new RuleResult("VELOCITY_EXCEEDED", score,
                    recentCount + " transactions in last " + VELOCITY_WINDOW_MINUTES + " minutes");
        }
        return RuleResult.clean("VELOCITY_EXCEEDED");
    }

    /** Rule 3 — Impossible travel (+25): Haversine > 500km in under 1 hour */
    private RuleResult ruleGeoAnomaly(Transaction tx) {
        if (tx.getLatitude() == null || tx.getLongitude() == null) {
            return RuleResult.clean("GEO_ANOMALY");
        }

        Optional<Transaction> lastTx = txRepo
                .findTopBySenderIdAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(
                        tx.getSender().getId(),
                        Transaction.TransactionStatus.COMPLETED,
                        LocalDateTime.now().minusHours(1));

        if (lastTx.isPresent()
                && lastTx.get().getLatitude() != null
                && lastTx.get().getLongitude() != null) {

            double km = haversineKm(
                    tx.getLatitude(), tx.getLongitude(),
                    lastTx.get().getLatitude(), lastTx.get().getLongitude());

            if (km > GEO_ANOMALY_KM) {
                return new RuleResult("GEO_ANOMALY", 25,
                        String.format("Impossible travel: %.0f km in under 1 hour", km));
            }
        }
        return RuleResult.clean("GEO_ANOMALY");
    }

    /** Rule 4 — Unknown device (+15): device not in user's known profile */
    private RuleResult ruleUnknownDevice(Transaction tx) {
        if (tx.getDeviceId() == null) return RuleResult.clean("UNKNOWN_DEVICE");

        Optional<UserBehaviourProfile> profile = profileRepo.findById(tx.getSender().getId());
        if (profile.isPresent()) {
            String known = profile.get().getKnownDeviceIds();
            if (known != null && !known.contains(tx.getDeviceId())) {
                return new RuleResult("UNKNOWN_DEVICE", 15,
                        "Device ID not in known devices for user");
            }
        }
        return RuleResult.clean("UNKNOWN_DEVICE");
    }

    /** Rule 5 — Unusual hour (+10): 03:00-05:00 correlates with automated fraud scripts */
    private RuleResult ruleUnusualHour(Transaction tx) {
        int hour = LocalDateTime.now().getHour();
        if (hour >= 3 && hour < 5) {
            return new RuleResult("UNUSUAL_HOUR", 10,
                    "Transaction at suspicious hour: " + hour + ":00");
        }
        return RuleResult.clean("UNUSUAL_HOUR");
    }

    /** Rule 6 — Amount anomaly (+15-20): z-score > 3 standard deviations from user mean */
    private RuleResult ruleAmountAnomaly(Transaction tx) {
        Optional<UserBehaviourProfile> profile = profileRepo.findById(tx.getSender().getId());
        if (profile.isPresent()) {
            BigDecimal avg = profile.get().getAvgTxAmount30d();
            if (avg != null && avg.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal threshold = avg.multiply(UNUSUAL_AMOUNT_MULTIPLIER);
                if (tx.getAmount().compareTo(threshold) > 0) {
                    BigDecimal stdDev = profile.get().getStdDevAmount30d();
                    int score = 15;
                    if (stdDev != null && stdDev.compareTo(BigDecimal.ZERO) > 0) {
                        double zScore = tx.getAmount().subtract(avg)
                                .divide(stdDev, 4, RoundingMode.HALF_UP)
                                .doubleValue();
                        score = (int) Math.min(20, 10 + zScore * 2);
                    }
                    return new RuleResult("AMOUNT_ANOMALY", score,
                            "Amount is " + tx.getAmount().divide(avg, 1, RoundingMode.HALF_UP)
                                    + "x user 30-day average");
                }
            }
        }
        return RuleResult.clean("AMOUNT_ANOMALY");
    }

    /** Rule 7 — New receiver (+10): account created within 7 days = mule account indicator */
    private RuleResult ruleNewReceiver(Transaction tx) {
        if (tx.getReceiver().getCreatedAt() != null
                && tx.getReceiver().getCreatedAt().isAfter(LocalDateTime.now().minusDays(7))) {
            return new RuleResult("NEW_RECEIVER_ACCOUNT", 10,
                    "Receiver account created within last 7 days");
        }
        return RuleResult.clean("NEW_RECEIVER_ACCOUNT");
    }

    // ──────────────────────────────────────────────────────────────
    //  HELPERS
    // ──────────────────────────────────────────────────────────────

    private Action determineAction(int score) {
        if (score >= BLOCK_THRESHOLD) return Action.BLOCK;
        if (score >= FLAG_THRESHOLD)  return Action.REVIEW;
        if (score >= 40)              return Action.MONITOR;
        return Action.ALLOW;
    }

    /** Haversine formula: great-circle distance in kilometres between two coordinates */
    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // ──────────────────────────────────────────────────────────────
    //  VALUE OBJECTS
    // ──────────────────────────────────────────────────────────────

    public record FraudEvaluation(
            int              score,
            boolean          flagged,
            String           flagReason,
            Action           action,
            List<RuleResult> ruleBreakdown
    ) {}

    public record RuleResult(String ruleName, int score, String detail) {
        static RuleResult clean(String name) {
            return new RuleResult(name, 0, null);
        }
    }

    public enum Action {
        ALLOW, MONITOR, REVIEW, BLOCK
    }
}
