package dev.jbyanx.finsight.domain.telegram.repository;

import dev.jbyanx.finsight.domain.telegram.entity.LinkCode;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LinkCodeRepository extends JpaRepository<LinkCode, String> {
    Optional<LinkCode> findByCodeAndUsedFalse(String code);
}