package dev.jbyanx.finsight.domain.debt.repository;

import dev.jbyanx.finsight.domain.debt.entity.FixedDebt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface FixedDebtRepository extends JpaRepository<FixedDebt, String> {

    List<FixedDebt> findAllByUserIdAndActiveTrue(String userId);

    Optional<FixedDebt> findByIdAndUserId(String id, String userId);

    // explícitos documenta la intención y evita depender de ese comportamiento
    @Query("SELECT d FROM FixedDebt d WHERE d.user.id = :userId AND d.active = true " +
            "AND d.paymentDueDay IS NOT NULL AND (" +
            "(:fromDay <= :toDay AND d.paymentDueDay BETWEEN :fromDay AND :toDay) OR " +
            "(:fromDay > :toDay AND (d.paymentDueDay >= :fromDay OR d.paymentDueDay <= :toDay)))")
    List<FixedDebt> findUpcomingByUserId(String userId, int fromDay, int toDay);

    // Total de deuda restante del usuario
    @Query("SELECT COALESCE(SUM(d.remainingBalance), 0) FROM FixedDebt d " +
            "WHERE d.user.id = :userId AND d.active = true")
    java.math.BigDecimal sumRemainingBalanceByUserId(String userId);
}