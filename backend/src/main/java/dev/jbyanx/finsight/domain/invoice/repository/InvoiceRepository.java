package dev.jbyanx.finsight.domain.invoice.repository;

import dev.jbyanx.finsight.domain.invoice.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<Invoice, String> {

    List<Invoice> findAllByUserIdAndActiveTrue(String userId);

    Optional<Invoice> findByIdAndUserId(String id, String userId);

    // Facturas que vencen en X días — para alertas
    @Query("SELECT i FROM Invoice i WHERE i.user.id = :userId " +
            "AND i.active = true " +
            "AND i.dueDay BETWEEN :fromDay AND :toDay")
    List<Invoice> findUpcomingByUserId(String userId, int fromDay, int toDay);

    // Facturas que debo cobrar a otros
    List<Invoice> findAllByUserIdAndIsOwnFalseAndActiveTrue(String userId);
}