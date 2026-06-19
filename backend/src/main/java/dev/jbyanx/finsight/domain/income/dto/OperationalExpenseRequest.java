package dev.jbyanx.finsight.domain.income.dto;

import dev.jbyanx.finsight.domain.income.entity.IncomeSource.IncomeFrequency;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class OperationalExpenseRequest {

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Frequency is required")
    private IncomeFrequency frequency;

    private Integer occurrencesPerMonth;

    @NotNull(message = "Estimated amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal estimatedAmount;

    private String incomeSourceId;
}