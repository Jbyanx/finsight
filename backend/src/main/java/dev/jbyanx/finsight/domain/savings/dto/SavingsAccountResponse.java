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

    /** Saldo físico total de la cuenta (incluye dinero de terceros). */
    private BigDecimal balance;

    /** Dinero de terceros alojado en la cuenta (bolsillos compartidos). */
    private BigDecimal sharedTotal;

    /** Patrimonio REAL del usuario en esta cuenta: balance - sharedTotal. */
    private BigDecimal ownBalance;

    /** Dinero libre: ni de terceros ni reservado en bolsillos propios. */
    private BigDecimal availableBalance;

    /** true = la cuenta entera es de terceros / conjunta. */
    private boolean shared;
    private String sharedOwnerName;

    private List<PocketResponse> pockets;
    private LocalDateTime createdAt;
}