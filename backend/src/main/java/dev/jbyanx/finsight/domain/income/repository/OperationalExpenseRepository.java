package dev.jbyanx.finsight.domain.income.repository;

import dev.jbyanx.finsight.domain.income.entity.OperationalExpense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface OperationalExpenseRepository extends JpaRepository<OperationalExpense, String> {

    List<OperationalExpense> findAllByUserIdAndActiveTrue(String userId);

    Optional<OperationalExpense> findByIdAndUserId(String id, String userId);

    @Query("SELECT COALESCE(SUM(e.estimatedAmount), 0) FROM OperationalExpense e " +
            "WHERE e.user.id = :userId AND e.active = true " +
            "AND e.frequency = 'PER_SHIFT'")
    BigDecimal sumPerShiftExpensesByUserId(String userId);
}