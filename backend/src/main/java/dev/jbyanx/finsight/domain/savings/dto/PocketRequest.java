package dev.jbyanx.finsight.domain.savings.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class PocketRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @PositiveOrZero(message = "Balance cannot be negative")
    private BigDecimal balance;

    private BigDecimal goalAmount;

    /** true = el dinero de este bolsillo es de un tercero (no cuenta como del usuario). */
    private boolean shared;

    /** Dueño real cuando shared = true. Ej: "Papá". */
    private String sharedOwnerName;
}