package com.finanalytics.finanalytics_platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finanalytics.finanalytics_platform.entity.Transaction;
import com.finanalytics.finanalytics_platform.entity.UserBehaviourProfile;
import com.finanalytics.finanalytics_platform.repository.TransactionRepository;
import com.finanalytics.finanalytics_platform.repository.UserBehaviourProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Maintains a rolling 30-day statistical fingerprint per user.
 * Called asynchronously from TransactionService after each completed transaction.
 * Feeds the fraud engine's Rule 4 (unknown device) and Rule 6 (amount anomaly).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BehaviourProfileService {

    private final TransactionRepository          txRepo;
    private final UserBehaviourProfileRepository profileRepo;
    private final ObjectMapper                   objectMapper;

    @Transactional
    public void recalculate(Long userId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        List<Transaction> recent = txRepo.findBySenderIdAndStatusAndCreatedAtAfter(
                userId,
                Transaction.TransactionStatus.COMPLETED,
                thirtyDaysAgo
        );

        if (recent.isEmpty()) return;

        // Mean
        BigDecimal sum  = recent.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal mean = sum.divide(BigDecimal.valueOf(recent.size()), 4, RoundingMode.HALF_UP);

        // Standard deviation: sigma = sqrt(SUM((xi - mean)^2) / n)
        BigDecimal variance = recent.stream()
                .map(t -> t.getAmount().subtract(mean).pow(2))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(recent.size()), 4, RoundingMode.HALF_UP);
        BigDecimal stdDev = variance.sqrt(new MathContext(10));

        // Max single transaction
        BigDecimal max = recent.stream()
                .map(Transaction::getAmount)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        // Known device IDs
        Set<String> devices = new HashSet<>();
        recent.stream()
                .map(Transaction::getDeviceId)
                .filter(Objects::nonNull)
                .forEach(devices::add);

        // Usual countries
        Set<String> countries = new HashSet<>();
        recent.stream()
                .map(Transaction::getLocationCountry)
                .filter(Objects::nonNull)
                .forEach(countries::add);

        double avgDaily = (double) recent.size() / 30.0;

        // Upsert profile
        UserBehaviourProfile profile = profileRepo.findById(userId)
                .orElse(new UserBehaviourProfile());
        profile.setUserId(userId);
        profile.setAvgTxAmount30d(mean);
        profile.setStdDevAmount30d(stdDev);
        profile.setMaxSingleTx30d(max);
        profile.setAvgDailyTxCount30d(avgDaily);
        profile.setTotalTxCount((long) recent.size());
        profile.setUsualCountries(String.join(",", countries));
        profile.setLastUpdated(LocalDateTime.now());

        try {
            profile.setKnownDeviceIds(
                    objectMapper.writeValueAsString(new ArrayList<>(devices)));
        } catch (Exception e) {
            log.warn("Could not serialise device IDs for userId={}", userId);
        }

        profileRepo.save(profile);
        log.debug("BEHAVIOUR_PROFILE_UPDATED userId={} mean={} stdDev={}", userId, mean, stdDev);
    }
}