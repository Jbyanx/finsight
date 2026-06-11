package dev.jbyanx.finsight.domain.card.repository;

import dev.jbyanx.finsight.domain.card.entity.CreditCard;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface CreditCardRepository extends JpaRepository<CreditCard, String> {

    List<CreditCard> findAllByUserIdAndActiveTrue(String userId);

    @EntityGraph(attributePaths = {"user"})
    Optional<CreditCard> findByIdAndUserId(String id, String userId);

    boolean existsByInstitutionAndUserIdAndActiveTrue(String institution, String userId);

    @Query("SELECT c FROM CreditCard c WHERE c.user.id = :userId " +
            "AND c.active = true " +
            "ORDER BY c.usedAmount / c.creditLimit ASC")
    List<CreditCard> findAllByUserIdOrderByUtilizationAsc(String userId);
}