package dev.jbyanx.finsight.domain.income.dto;

import dev.jbyanx.finsight.domain.income.entity.IncomeSource.IncomeFrequency;
import dev.jbyanx.finsight.domain.income.entity.IncomeSource.IncomeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class IncomeSourceRequest {

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Type is required")
    private IncomeType type;

    private BigDecimal estimatedAmount;
    private IncomeFrequency frequency;
}