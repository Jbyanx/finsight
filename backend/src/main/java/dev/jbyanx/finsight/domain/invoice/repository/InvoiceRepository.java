package dev.jbyanx.finsight.domain.invoice.repository;

import dev.jbyanx.finsight.domain.invoice.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, String> {

    List<Invoice> findAllByUserIdAndActiveTrue(String userId);

    Optional<Invoice> findByIdAndUserId(String id, String userId);

    // Facturas que debo cobrar a otros
    List<Invoice> findAllByUserIdAndOwnFalseAndActiveTrue(String userId);
}