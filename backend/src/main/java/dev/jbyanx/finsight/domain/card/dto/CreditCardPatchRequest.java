package dev.jbyanx.finsight.domain.card.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreditCardPatchRequest {

    private String nickname;

    @DecimalMin(value = "0.01")
    private BigDecimal creditLimit;

    @Min(1) @Max(31)
    private Integer cutOffDay;

    @Min(1) @Max(31)
    private Integer paymentDueDay;
}