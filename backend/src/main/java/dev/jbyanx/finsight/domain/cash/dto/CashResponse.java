package dev.jbyanx.finsight.domain.cash.dto;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class CashResponse {
    private String id;
    private BigDecimal balance;
    private LocalDateTime updatedAt;
}