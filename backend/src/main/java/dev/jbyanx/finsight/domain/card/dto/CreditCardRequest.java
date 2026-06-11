package dev.jbyanx.finsight.domain.card.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class CreditCardRequest {

    @NotBlank(message = "Institution is required")
    private String institution;

    private String nickname;

    @NotNull(message = "Credit limit is required")
    @DecimalMin(value = "0.01", message = "Credit limit must be greater than 0")
    private BigDecimal creditLimit;

    @NotNull(message = "Cut off day is required")
    @Min(value = 1, message = "Cut off day must be between 1 and 31")
    @Max(value = 31, message = "Cut off day must be between 1 and 31")
    private Integer cutOffDay;

    @NotNull(message = "Payment due day is required")
    @Min(value = 1, message = "Payment due day must be between 1 and 31")
    @Max(value = 31, message = "Payment due day must be between 1 and 31")
    private Integer paymentDueDay;
}