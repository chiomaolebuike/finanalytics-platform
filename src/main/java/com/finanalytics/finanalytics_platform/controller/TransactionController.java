package com.finanalytics.finanalytics_platform.controller;


import com.finanalytics.finanalytics_platform.dto.TransactionRequest;
import com.finanalytics.finanalytics_platform.dto.TransactionResponse;
import com.finanalytics.finanalytics_platform.entity.Transaction;
import com.finanalytics.finanalytics_platform.service.TransactionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService txService;

    /**
     * POST /api/transactions/submit
     * Submit a new transaction. Runs fraud engine before persisting.
     * Returns 402 Payment Required if transaction is BLOCKED.
     */
    @PostMapping("/submit")
    public ResponseEntity<TransactionResponse> submit(
            @Valid @RequestBody TransactionRequest req,
            @AuthenticationPrincipal String email,
            HttpServletRequest httpReq) {

        TransactionResponse result = txService.submit(req, email, httpReq);

        if (result.blocked()) {
            return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(result);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * GET /api/transactions
     * Paginated personal transaction history.
     */
    @GetMapping
    public Page<TransactionResponse> history(
            @AuthenticationPrincipal String email,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return txService.history(email, page, size);
    }

    /**
     * GET /api/transactions/{referenceId}
     * Get a single transaction. Only owner or ADMIN may access.
     */
    @GetMapping("/{referenceId}")
    public TransactionResponse detail(
            @PathVariable String referenceId,
            @AuthenticationPrincipal String email) {
        return txService.getByReference(referenceId, email);
    }

    /**
     * GET /api/transactions/flagged
     * All flagged or blocked transactions — ADMIN only.
     */
    @GetMapping("/flagged")
    @PreAuthorize("hasRole('ADMIN')")
    public Page<TransactionResponse> flagged(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "50") int size) {
        return txService.flaggedTransactions(page, size);
    }

    /**
     * PATCH /api/transactions/{id}/review
     * Update fraud review decision — ADMIN only.
     */
    @PatchMapping("/{id}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateReview(
            @PathVariable Long id,
            @RequestParam Transaction.FraudReviewStatus status,
            @AuthenticationPrincipal String adminEmail) {
        txService.updateFraudReview(id, status, adminEmail);
        return ResponseEntity.noContent().build();
    }
}