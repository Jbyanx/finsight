package dev.jbyanx.finsight.domain.debt.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class FixedDebtPatchRequest {
    private String description;

    @DecimalMin(value = "0.01")
    private BigDecimal monthlyPayment;

    @Min(1) @Max(31)
    private Integer paymentDueDay;
}