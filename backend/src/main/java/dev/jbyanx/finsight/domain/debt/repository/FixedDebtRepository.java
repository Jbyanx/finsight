package dev.jbyanx.finsight.domain.debt.repository;

import dev.jbyanx.finsight.domain.debt.entity.FixedDebt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface FixedDebtRepository extends JpaRepository<FixedDebt, String> {

    List<FixedDebt> findAllByUserIdAndActiveTrue(String userId);

    Optional<FixedDebt> findByIdAndUserId(String id, String userId);

    // Deudas que vencen en X días — para alertas pre-corte
    @Query("SELECT d FROM FixedDebt d WHERE d.user.id = :userId " +
            "AND d.active = true " +
            "AND d.paymentDueDay BETWEEN :fromDay AND :toDay")
    List<FixedDebt> findUpcomingByUserId(String userId, int fromDay, int toDay);

    // Total de deuda restante del usuario
    @Query("SELECT COALESCE(SUM(d.remainingBalance), 0) FROM FixedDebt d " +
            "WHERE d.user.id = :userId AND d.active = true")
    java.math.BigDecimal sumRemainingBalanceByUserId(String userId);
}