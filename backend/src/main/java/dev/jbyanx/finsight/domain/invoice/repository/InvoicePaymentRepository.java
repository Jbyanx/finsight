package dev.jbyanx.finsight.domain.invoice.repository;

import dev.jbyanx.finsight.domain.invoice.entity.InvoicePayment;
import dev.jbyanx.finsight.domain.invoice.entity.InvoicePayment.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
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

    /**
     * Períodos ya pagados de un conjunto de facturas, en una sola consulta.
     *
     * Se usa para no alertar una factura que el usuario ya pagó. Devuelve una
     * proyección (id + período) en vez de entidades a propósito: solo se necesitan
     * tres columnas y así se evita tocar el proxy LAZY de {@code p.invoice} para
     * leerle el id.
     */
    @Query("SELECT p.invoice.id AS invoiceId, " +
            "       p.periodYear AS periodYear, " +
            "       p.periodMonth AS periodMonth " +
            "FROM InvoicePayment p " +
            "WHERE p.invoice.id IN :invoiceIds AND p.status = :status")
    List<PaidPeriod> findPaidPeriods(@Param("invoiceIds") Collection<String> invoiceIds,
                                     @Param("status") PaymentStatus status);

    /** Proyección de {@link #findPaidPeriods}. */
    interface PaidPeriod {
        String getInvoiceId();
        Integer getPeriodYear();
        Integer getPeriodMonth();
    }
}