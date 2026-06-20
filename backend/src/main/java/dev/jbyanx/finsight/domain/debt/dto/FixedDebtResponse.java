package dev.jbyanx.finsight.domain.debt.dto;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class FixedDebtResponse {
    private String id;
    private String institution;
    private String description;
    private BigDecimal totalAmount;
    private BigDecimal monthlyPayment;
    private BigDecimal interestRate;
    private Integer paymentDueDay;
    private BigDecimal remainingBalance;
    private BigDecimal paidAmount;
    private BigDecimal progressPercentage;
    private boolean paidOff;
    private LocalDateTime createdAt;
}