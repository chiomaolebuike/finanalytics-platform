package com.finanalytics.finanalytics_platform.controller;

import com.finanalytics.finanalytics_platform.dto.AnalyticsSummaryDto;
import com.finanalytics.finanalytics_platform.dto.RiskProfileDto;
import com.finanalytics.finanalytics_platform.dto.SpendingTrendDto;
import com.finanalytics.finanalytics_platform.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /**
     * GET /api/analytics/summary?from=2024-01-01&to=2024-12-31
     * Total spend, average order value, failure rate, category breakdown.
     */
    @GetMapping("/summary")
    public AnalyticsSummaryDto summary(
            @AuthenticationPrincipal String email,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return analyticsService.summary(email, from, to);
    }

    /**
     * GET /api/analytics/trend
     * 6-month monthly spending trend. Cached per user (5 minutes).
     */
    @GetMapping("/trend")
    @Cacheable(value = "trend", key = "#email")
    public List<SpendingTrendDto> trend(@AuthenticationPrincipal String email) {
        return analyticsService.monthlyTrend(email);
    }

    /**
     * GET /api/analytics/risk-profile
     * Current user's fraud rate and risk level.
     */
    @GetMapping("/risk-profile")
    public RiskProfileDto riskProfile(@AuthenticationPrincipal String email) {
        return analyticsService.riskProfile(email);
    }

    /**
     * GET /api/analytics/risk-distribution
     * Count of users per risk level — ADMIN only.
     */
    @GetMapping("/risk-distribution")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Long> riskDistribution() {
        return analyticsService.riskDistribution();
    }
}