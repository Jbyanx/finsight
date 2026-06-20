package dev.jbyanx.finsight.domain.income.repository;

import dev.jbyanx.finsight.domain.income.entity.IncomeSource;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface IncomeSourceRepository extends JpaRepository<IncomeSource, String> {

    List<IncomeSource> findAllByUserIdAndActiveTrue(String userId);

    Optional<IncomeSource> findByIdAndUserId(String id, String userId);
}