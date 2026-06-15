package dev.jbyanx.finsight.domain.savings.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class SavingsAccountRequest {

    @NotBlank(message = "Institution is required")
    private String institution;

    private String nickname;

    @PositiveOrZero(message = "Balance cannot be negative")
    private BigDecimal balance;

    private boolean shared;

    private String sharedOwnerName;
}