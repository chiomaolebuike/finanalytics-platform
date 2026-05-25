package com.finanalytics.finanalytics_platform.repository;

import com.finanalytics.finanalytics_platform.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * All queries use JPA parameterised binding — SQL injection is prevented by construction.
 * User input is NEVER concatenated into query strings.
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    Optional<Transaction> findByReferenceId(String referenceId);

    // Paginated transaction history — own transactions only
    Page<Transaction> findBySenderIdOrReceiverId(Long senderId, Long receiverId, Pageable pageable);

    // All flagged transactions for admin dashboard
    Page<Transaction> findByIsFlaggedTrue(Pageable pageable);

    // Count recent transactions for velocity rule
    long countBySenderIdAndCreatedAtAfter(Long senderId, LocalDateTime since);

    // Last transaction in window for geo anomaly rule
    Optional<Transaction> findTopBySenderIdAndStatusAndCreatedAtAfterOrderByCreatedAtDesc(
            Long senderId,
            Transaction.TransactionStatus status,
            LocalDateTime since
    );

    // Sum of completed transactions in a date range
    @Query("""
            SELECT SUM(t.amount) FROM Transaction t
            WHERE t.sender.id = :uid
              AND t.status = 'COMPLETED'
              AND t.createdAt BETWEEN :from AND :to
            """)
    Optional<BigDecimal> sumCompleted(
            @Param("uid")  Long uid,
            @Param("from") LocalDateTime from,
            @Param("to")   LocalDateTime to
    );

    // Average order value
    @Query("SELECT AVG(t.amount) FROM Transaction t WHERE t.sender.id = :uid AND t.status = 'COMPLETED'")
    Optional<BigDecimal> avgAmount(@Param("uid") Long uid);

    // Spend per merchant category
    @Query("""
            SELECT t.merchantCategory, SUM(t.amount)
            FROM Transaction t
            WHERE t.sender.id = :uid AND t.status = 'COMPLETED'
            GROUP BY t.merchantCategory
            """)
    List<Object[]> spendByCategory(@Param("uid") Long uid);

    long countBySenderId(Long senderId);

    long countBySenderIdAndStatus(Long senderId, Transaction.TransactionStatus status);

    long countByIsFlaggedTrue();

    // Transactions in date window for behaviour profile recalculation
    List<Transaction> findBySenderIdAndStatusAndCreatedAtAfter(
            Long senderId,
            Transaction.TransactionStatus status,
            LocalDateTime since
    );
}