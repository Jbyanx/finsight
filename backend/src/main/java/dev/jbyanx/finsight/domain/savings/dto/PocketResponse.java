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

    /** true = dinero de un tercero. No suma al patrimonio del usuario. */
    private boolean shared;
    private String sharedOwnerName;

    private LocalDateTime createdAt;
}