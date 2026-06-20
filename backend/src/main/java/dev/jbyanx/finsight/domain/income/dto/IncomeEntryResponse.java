package dev.jbyanx.finsight.domain.income.dto;

import dev.jbyanx.finsight.domain.income.entity.IncomeEntry.DestinationType;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class IncomeEntryResponse {
    private String id;
    private BigDecimal amount;
    private String description;
    private String incomeSourceDescription;
    private DestinationType destinationType;
    private String destinationName;
    private LocalDateTime receivedAt;
    private LocalDateTime createdAt;
}