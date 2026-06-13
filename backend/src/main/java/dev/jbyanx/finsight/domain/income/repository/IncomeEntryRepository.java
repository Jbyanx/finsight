package dev.jbyanx.finsight.domain.income.repository;

import dev.jbyanx.finsight.domain.income.entity.IncomeEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface IncomeEntryRepository extends JpaRepository<IncomeEntry, String> {

    Page<IncomeEntry> findAllByUserIdOrderByReceivedAtDesc(String userId, Pageable pageable);

    @Query("SELECT SUM(e.amount) FROM IncomeEntry e WHERE e.user.id = :userId " +
            "AND e.receivedAt BETWEEN :from AND :to")
    BigDecimal sumByUserIdAndReceivedAtBetween(String userId,
                                               LocalDateTime from, LocalDateTime to);
}