package dev.jbyanx.finsight.domain.savings.repository;

import dev.jbyanx.finsight.domain.savings.entity.Pocket;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PocketRepository extends JpaRepository<Pocket, String> {

    List<Pocket> findAllBySavingsAccountIdAndActiveTrue(String savingsAccountId);

    Optional<Pocket> findByIdAndSavingsAccountId(String id, String savingsAccountId);

    boolean existsByNameAndSavingsAccountIdAndActiveTrue(String name, String savingsAccountId);
}