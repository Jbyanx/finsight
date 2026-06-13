package dev.jbyanx.finsight.domain.income.dto;

import dev.jbyanx.finsight.domain.income.entity.IncomeSource.IncomeFrequency;
import dev.jbyanx.finsight.domain.income.entity.IncomeSource.IncomeType;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class IncomeSourceResponse {
    private String id;
    private String description;
    private IncomeType type;
    private BigDecimal estimatedAmount;
    private IncomeFrequency frequency;
    private LocalDateTime createdAt;
}