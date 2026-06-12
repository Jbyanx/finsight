package dev.jbyanx.finsight.domain.savings.dto;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class PocketResponse {

    private String id;
    private String name;
    private BigDecimal balance;
    private BigDecimal goalAmount;
    private BigDecimal progressPercentage;
    private LocalDateTime createdAt;
}