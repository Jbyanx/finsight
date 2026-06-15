package dev.jbyanx.finsight.domain.financial.dto;

import dev.jbyanx.finsight.domain.card.dto.UtilizationStatus;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;

@Getter
@Builder
public class CardAlertResponse {
    private String cardId;
    private String institution;
    private String nickname;
    private BigDecimal utilizationPercentage;
    private UtilizationStatus status;
    private BigDecimal recommendedPayment; // cuánto abonar para bajar al 30%
}