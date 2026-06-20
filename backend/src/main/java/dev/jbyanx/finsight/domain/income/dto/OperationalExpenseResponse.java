package dev.jbyanx.finsight.domain.income.dto;

import dev.jbyanx.finsight.domain.income.entity.IncomeSource.IncomeFrequency;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class OperationalExpenseResponse {
    private String id;
    private String description;
    private IncomeFrequency frequency;
    private Integer occurrencesPerMonth;
    private BigDecimal estimatedAmount;
    private String incomeSourceDescription;
    private LocalDateTime createdAt;
}