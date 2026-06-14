package com.finanalytics.finanalytics_platform.service;

import com.finanalytics.finanalytics_platform.entity.Transaction;
import com.finanalytics.finanalytics_platform.service.FraudAnalysisService.FraudEvaluation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 
 * Operates behind the scenes to maintain an immutable record of all system events and security checks, providing a clear paper trail for compliance and risk reporting.
 * PCI DSS Req 10: All authentication, transaction, and admin events must be logged.
 * Log format uses key=value pairs for easy ingestion by Splunk / ELK / CloudWatch.
 *
 * Security rules:
 * - NEVER log passwords, JWTs, or full card numbers
 * - ALWAYS mask email addresses to minimise PII in logs
 * - Strip newlines from all input to prevent log injection
 */
@Service
@Slf4j
public class AuditLogService {

    public void logFraudBlock(Transaction tx, FraudEvaluation eval) {
        log.warn("FRAUD_EVENT event=BLOCKED ref={} senderId={} amount={} currency={} score={} reason=\"{}\"",
                tx.getReferenceId(),
                tx.getSender().getId(),
                tx.getAmount(),
                tx.getCurrency(),
                eval.score(),
                sanitise(eval.flagReason()));
    }

    public void logFraudReview(Transaction tx, FraudEvaluation eval) {
        log.warn("FRAUD_EVENT event=FLAGGED_FOR_REVIEW ref={} senderId={} amount={} score={} reason=\"{}\"",
                tx.getReferenceId(),
                tx.getSender().getId(),
                tx.getAmount(),
                eval.score(),
                sanitise(eval.flagReason()));
    }

    public void logReviewDecision(Transaction tx,
                                   Transaction.FraudReviewStatus status,
                                   String adminEmail) {
        log.info("FRAUD_EVENT event=REVIEW_DECISION ref={} decision={} admin={}",
                tx.getReferenceId(), status, maskEmail(adminEmail));
    }

    public void logSuspiciousLogin(String email, String ip) {
        log.warn("SECURITY_EVENT event=SUSPICIOUS_LOGIN email={} ip={}", maskEmail(email), ip);
    }

    /** Strip newlines to prevent log injection attacks */
    private String sanitise(String input) {
        if (input == null) return "";
        return input.replaceAll("[\n\r]", " ")
                    .substring(0, Math.min(input.length(), 200));
    }

    /** Mask email: alice@example.com → al***@example.com */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        String[] parts = email.split("@");
        String local = parts[0];
        return local.substring(0, Math.min(2, local.length())) + "***@" + parts[1];
    }
}