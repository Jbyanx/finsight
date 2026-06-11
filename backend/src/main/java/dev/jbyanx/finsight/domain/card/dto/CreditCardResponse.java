package dev.jbyanx.finsight.domain.card.dto;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class CreditCardResponse {

    private String id;
    private String institution;
    private String nickname;
    private BigDecimal creditLimit;
    private BigDecimal usedAmount;
    private BigDecimal availableAmount;
    private BigDecimal utilizationPercentage;
    private UtilizationStatus utilizationStatus; // SAFE | YELLOW | RED
    private Integer cutOffDay;
    private Integer paymentDueDay;
    private LocalDateTime createdAt;
}