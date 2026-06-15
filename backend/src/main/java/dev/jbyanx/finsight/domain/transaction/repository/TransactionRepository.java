package dev.jbyanx.finsight.domain.transaction.repository;

import dev.jbyanx.finsight.domain.transaction.entity.Transaction;
import dev.jbyanx.finsight.domain.transaction.entity.Transaction.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface TransactionRepository extends JpaRepository<Transaction, String> {

    Page<Transaction> findAllByUserIdOrderByDateDesc(String userId, Pageable pageable);

    Page<Transaction> findAllByUserIdAndCreditCardIdOrderByDateDesc(
            String userId, String cardId, Pageable pageable);

    Page<Transaction> findAllByUserIdAndSharedTrueOrderByDateDesc(
            String userId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.creditCard.id = :cardId " +
            "AND t.date BETWEEN :from AND :to " +
            "AND t.status = 'COMPLETED'")
    BigDecimal sumByCreditCardAndDateBetween(
            String cardId, LocalDateTime from, LocalDateTime to);

    @Query("SELECT t FROM Transaction t WHERE t.user.id = :userId " +
            "AND t.shared = true " +
            "AND t.paymentDeadline < :now " +
            "AND t.status = 'PENDING'")
    Page<Transaction> findOverdueSharedTransactions(
            String userId, LocalDateTime now, Pageable pageable);
}