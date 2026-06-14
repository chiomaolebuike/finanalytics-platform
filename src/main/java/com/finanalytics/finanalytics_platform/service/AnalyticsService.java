package com.finanalytics.finanalytics_platform.service;

import com.finanalytics.finanalytics_platform.dto.AnalyticsSummaryDto;
import com.finanalytics.finanalytics_platform.dto.RiskProfileDto;
import com.finanalytics.finanalytics_platform.dto.SpendingTrendDto;
import com.finanalytics.finanalytics_platform.entity.Transaction;
import com.finanalytics.finanalytics_platform.entity.User;
import com.finanalytics.finanalytics_platform.exception.ResourceNotFoundException;
import com.finanalytics.finanalytics_platform.repository.TransactionRepository;
import com.finanalytics.finanalytics_platform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// Ensures that a comprehensive suite of analytics is available to users and admins, covering spending patterns, risk profiles, and fraud trends.
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final TransactionRepository txRepo;
    private final UserRepository        userRepo;

    public AnalyticsSummaryDto summary(String email, LocalDate from, LocalDate to) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        BigDecimal totalSpend = txRepo.sumCompleted(
                        user.getId(),
                        from.atStartOfDay(),
                        to.atTime(23, 59, 59))
                .orElse(BigDecimal.ZERO);

        BigDecimal avgOrderValue = txRepo.avgAmount(user.getId())
                .orElse(BigDecimal.ZERO);

        long total  = txRepo.countBySenderId(user.getId());
        long failed = txRepo.countBySenderIdAndStatus(
                user.getId(), Transaction.TransactionStatus.FAILED);
        double failureRate = total == 0 ? 0.0 : (double) failed / total * 100;

        Map<String, BigDecimal> categoryBreakdown = txRepo.spendByCategory(user.getId())
                .stream()
                .filter(row -> row[0] != null)
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (BigDecimal) row[1]
                ));

        return new AnalyticsSummaryDto(totalSpend, avgOrderValue, failureRate, categoryBreakdown);
    }

    public List<SpendingTrendDto> monthlyTrend(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Group completed transactions by month over last 6 months
        return txRepo.findBySenderIdAndStatusAndCreatedAtAfter(
                        user.getId(),
                        Transaction.TransactionStatus.COMPLETED,
                        java.time.LocalDateTime.now().minusMonths(6))
                .stream()
                .collect(Collectors.groupingBy(
                        t -> t.getCreatedAt().getYear() + "-"
                                + String.format("%02d", t.getCreatedAt().getMonthValue()),
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
                ))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new SpendingTrendDto(e.getKey(), e.getValue()))
                .toList();
    }

    public RiskProfileDto riskProfile(String email) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        long total   = txRepo.countBySenderId(user.getId());
        long flagged = txRepo.countByIsFlaggedTrue();

        double fraudRate = total == 0 ? 0.0 : (double) flagged / total * 100;

        return new RiskProfileDto(
                email,
                user.getRiskLevel().name(),
                (int) total,
                (int) flagged,
                fraudRate
        );
    }

    public Map<String, Long> riskDistribution() {
        return Map.of(
                "LOW",      userRepo.countByRiskLevel(User.RiskLevel.LOW),
                "MEDIUM",   userRepo.countByRiskLevel(User.RiskLevel.MEDIUM),
                "HIGH",     userRepo.countByRiskLevel(User.RiskLevel.HIGH),
                "CRITICAL", userRepo.countByRiskLevel(User.RiskLevel.CRITICAL)
        );
    }
}