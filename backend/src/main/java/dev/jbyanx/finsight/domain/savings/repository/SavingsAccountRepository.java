package dev.jbyanx.finsight.domain.savings.repository;

import dev.jbyanx.finsight.domain.savings.entity.SavingsAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import java.util.List;
import java.util.Optional;

public interface SavingsAccountRepository extends JpaRepository<SavingsAccount, String> {

    List<SavingsAccount> findAllByUserIdAndActiveTrue(String userId);

    @EntityGraph(attributePaths = {"pockets"})
    Optional<SavingsAccount> findByIdAndUserId(String id, String userId);

    boolean existsByInstitutionAndUserIdAndActiveTrue(String institution, String userId);
}