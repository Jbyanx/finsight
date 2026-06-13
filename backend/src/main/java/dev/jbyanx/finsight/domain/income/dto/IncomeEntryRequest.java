package dev.jbyanx.finsight.domain.income.dto;

import dev.jbyanx.finsight.domain.income.entity.IncomeEntry.DestinationType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class IncomeEntryRequest {

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    private String description;
    private String incomeSourceId;
    private LocalDateTime receivedAt;

    @NotNull(message = "Destination type is required")
    private DestinationType destinationType;

    private String savingsAccountId;
    private String pocketId;
}