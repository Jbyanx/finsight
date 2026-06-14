package dev.jbyanx.finsight.domain.savings.dto;

import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class PocketPatchRequest {

    private String name;

    @PositiveOrZero
    private BigDecimal goalAmount;

    private boolean removeGoal; // true → elimina la meta
}