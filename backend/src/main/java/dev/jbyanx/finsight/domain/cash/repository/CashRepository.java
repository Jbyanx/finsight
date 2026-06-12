package dev.jbyanx.finsight.domain.cash.repository;

import dev.jbyanx.finsight.domain.cash.entity.Cash;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CashRepository extends JpaRepository<Cash, String> {

    Optional<Cash> findByUserId(String userId);
}