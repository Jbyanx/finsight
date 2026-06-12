package dev.jbyanx.finsight.domain.savings.dto;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class SavingsAccountResponse {

    private String id;
    private String institution;
    private String nickname;
    private BigDecimal balance;
    private BigDecimal availableBalance;
    private boolean isShared;
    private String sharedOwnerName;
    private List<PocketResponse> pockets;
    private LocalDateTime createdAt;
}