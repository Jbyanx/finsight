package dev.jbyanx.finsight.domain.income.dto;

import dev.jbyanx.finsight.domain.income.entity.IncomeSource.IncomeFrequency;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class OperationalExpensePatchRequest {
    private String description;
    private IncomeFrequency frequency;

    private Integer occurrencesPerMonth;

    @DecimalMin(value = "0.01")
    private BigDecimal estimatedAmount;
}