package com.finanalytics.finanalytics_platform.service;

import com.finanalytics.finanalytics_platform.dto.TransactionRequest;
import com.finanalytics.finanalytics_platform.dto.TransactionResponse;
import com.finanalytics.finanalytics_platform.entity.Transaction;
import com.finanalytics.finanalytics_platform.entity.User;
import com.finanalytics.finanalytics_platform.exception.ResourceNotFoundException;
import com.finanalytics.finanalytics_platform.repository.TransactionRepository;
import com.finanalytics.finanalytics_platform.repository.UserRepository;
import com.finanalytics.finanalytics_platform.service.FraudAnalysisService.FraudEvaluation;
import com.finanalytics.finanalytics_platform.service.FraudAnalysisService.Action;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository      txRepo;
    private final UserRepository             userRepo;
    private final FraudAnalysisService       fraudEngine;
    private final BehaviourProfileService    profileService;
    private final AuditLogService            auditLog;

    // ──────────────────────────────────────────────────────────────
    //  SUBMIT TRANSACTION
    // ──────────────────────────────────────────────────────────────

    /**
     * Full transaction pipeline:
     * 1. Validate sender and receiver exist
     * 2. Build Transaction entity from request
     * 3. Run fraud evaluation BEFORE persisting
     * 4. BLOCK → reject; REVIEW → persist with flag
     * 5. Persist to database
     * 6. Async: update behaviour profile
     */
    @Transactional
    public TransactionResponse submit(TransactionRequest req,
                                      String senderEmail,
                                      HttpServletRequest httpReq) {

        User sender = userRepo.findByEmail(senderEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));
        User receiver = userRepo.findById(req.receiverId())
                .orElseThrow(() -> new ResourceNotFoundException("Receiver not found: " + req.receiverId()));

        Transaction tx = Transaction.builder()
                .referenceId(UUID.randomUUID().toString())
                .sender(sender)
                .receiver(receiver)
                .amount(req.amount())
                .currency(req.currency() != null ? req.currency() : "ZAR")
                .paymentMethod(req.paymentMethod())
                .ipAddress(extractIp(httpReq))
                .deviceId(req.deviceId())
                .locationCountry(req.locationCountry())
                .locationCity(req.locationCity())
                .latitude(req.latitude())
                .longitude(req.longitude())
                .merchantName(req.merchantName())
                .merchantCategory(req.merchantCategory())
                .status(Transaction.TransactionStatus.PENDING)
                .build();

        // Run fraud engine
        FraudEvaluation eval = fraudEngine.evaluate(tx);
        tx.setRiskScore(eval.score());
        tx.setIsFlagged(eval.flagged());
        tx.setFlagReason(eval.flagReason());

        // Block immediately if score >= 80
        if (eval.action() == Action.BLOCK) {
            tx.setStatus(Transaction.TransactionStatus.BLOCKED);
            auditLog.logFraudBlock(tx, eval);
            Transaction saved = txRepo.save(tx);
            log.warn("TRANSACTION_BLOCKED ref={} score={}", tx.getReferenceId(), eval.score());
            return buildResponse(saved, eval, true);
        }

        // Flag for manual review if score 60-79
        if (eval.action() == Action.REVIEW) {
            tx.setFraudReviewStatus(Transaction.FraudReviewStatus.UNDER_REVIEW);
            auditLog.logFraudReview(tx, eval);
        }

        tx.setStatus(Transaction.TransactionStatus.COMPLETED);
        tx.setProcessedAt(LocalDateTime.now());
        Transaction saved = txRepo.save(tx);

        // Update behaviour profile asynchronously — does not block the response
        updateProfileAsync(sender.getId());

        return buildResponse(saved, eval, false);
    }

    // ──────────────────────────────────────────────────────────────
    //  QUERY METHODS
    // ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<TransactionResponse> history(String userEmail, int page, int size) {
        User user = userRepo.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return txRepo.findBySenderIdOrReceiverId(
                        user.getId(), user.getId(),
                        PageRequest.of(page, size, Sort.by("createdAt").descending()))
                .map(TransactionResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponse> flaggedTransactions(int page, int size) {
        return txRepo.findByIsFlaggedTrue(
                        PageRequest.of(page, size, Sort.by("riskScore").descending()))
                .map(TransactionResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public TransactionResponse getByReference(String referenceId, String callerEmail) {
        Transaction tx = txRepo.findByReferenceId(referenceId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + referenceId));

        // Security: only owner or admin may view a transaction
        boolean isOwner = tx.getSender().getEmail().equals(callerEmail)
                || tx.getReceiver().getEmail().equals(callerEmail);
        boolean isAdmin = userRepo.findByEmail(callerEmail)
                .map(u -> u.getRoles().contains("ROLE_ADMIN")).orElse(false);

        if (!isOwner && !isAdmin) {
            throw new SecurityException("Access denied to transaction " + referenceId);
        }
        return TransactionResponse.fromEntity(tx);
    }

    @Transactional
    public void updateFraudReview(Long txId,
                                   Transaction.FraudReviewStatus newStatus,
                                   String adminEmail) {
        Transaction tx = txRepo.findById(txId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found: " + txId));
        tx.setFraudReviewStatus(newStatus);
        if (newStatus == Transaction.FraudReviewStatus.CONFIRMED_FRAUD) {
            tx.setStatus(Transaction.TransactionStatus.BLOCKED);
        }
        txRepo.save(tx);
        auditLog.logReviewDecision(tx, newStatus, adminEmail);
    }

    // ──────────────────────────────────────────────────────────────
    //  HELPERS
    // ──────────────────────────────────────────────────────────────

    @Async
    protected void updateProfileAsync(Long userId) {
        try {
            profileService.recalculate(userId);
        } catch (Exception e) {
            log.error("Failed to update behaviour profile for userId={}: {}", userId, e.getMessage());
        }
    }

    /**
     * Extract client IP — respects X-Forwarded-For from nginx/ALB reverse proxies.
     * First IP in chain is the actual client.
     */
    private String extractIp(HttpServletRequest req) {
        String forwarded = req.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private TransactionResponse buildResponse(Transaction tx,
                                               FraudEvaluation eval,
                                               boolean blocked) {
        return new TransactionResponse(
                tx.getReferenceId(),
                tx.getAmount(),
                tx.getCurrency(),
                tx.getStatus().name(),
                eval.score(),
                eval.flagged(),
                eval.flagReason(),
                eval.action().name(),
                tx.getCreatedAt(),
                blocked
        );
    }
}
