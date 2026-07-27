package dev.jbyanx.finsight.domain.debt.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FixedDebtRequest {

    @NotBlank(message = "Institution is required")
    private String institution;

    private String description;

    @NotNull(message = "Total amount is required")
    @DecimalMin(value = "0.01", message = "Total amount must be greater than 0")
    private BigDecimal totalAmount;

    @DecimalMin(value = "0.01", message = "Monthly payment must be greater than 0")
    private BigDecimal monthlyPayment;

    private BigDecimal interestRate; // opcional, para cálculos avanzados

    @Min(1) @Max(31)
    private Integer paymentDueDay;

    // Opcional — si ya tiene algo pagado al registrar
    private BigDecimal remainingBalance;
}