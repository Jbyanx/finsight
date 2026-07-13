package dev.jbyanx.finsight.domain.transaction.dto;

import dev.jbyanx.finsight.domain.transaction.entity.Transaction.OriginType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    private String category;
    private LocalDateTime date;

    @NotNull(message = "Origin type is required")
    private OriginType originType;

    private String creditCardId;
    private String savingsAccountId;

    private boolean shared;
    private String sharedWithName;
    private BigDecimal sharedAmount;
    private LocalDateTime paymentDeadline;

    private Integer installments;
}