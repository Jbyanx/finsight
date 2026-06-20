package dev.jbyanx.finsight.domain.invoice.repository;

import dev.jbyanx.finsight.domain.invoice.entity.InvoicePayment;
import dev.jbyanx.finsight.domain.invoice.entity.InvoicePayment.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface InvoicePaymentRepository extends JpaRepository<InvoicePayment, String> {
    @Query("SELECT p FROM InvoicePayment p WHERE p.invoice.user.id = :userId " +
            "ORDER BY p.periodYear DESC, p.periodMonth DESC")
    Page<InvoicePayment> findAllByUserIdOrderByPeriodDesc(String userId, Pageable pageable);

    Page<InvoicePayment> findAllByInvoiceIdOrderByPeriodYearDescPeriodMonthDesc(
            String invoiceId, Pageable pageable);

    Optional<InvoicePayment> findByInvoiceIdAndPeriodMonthAndPeriodYear(
            String invoiceId, int month, int year);

    Page<InvoicePayment> findAllByInvoiceIdAndStatus(
            String invoiceId, PaymentStatus status, Pageable pageable);
}