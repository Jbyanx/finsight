package dev.jbyanx.finsight.domain.income.dto;

import dev.jbyanx.finsight.domain.income.entity.IncomeSource.IncomeFrequency;
import dev.jbyanx.finsight.domain.income.entity.IncomeSource.IncomeType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class IncomeSourcePatchRequest {
    private String description;
    private IncomeType type;
    private BigDecimal estimatedAmount;
    private IncomeFrequency frequency;
}