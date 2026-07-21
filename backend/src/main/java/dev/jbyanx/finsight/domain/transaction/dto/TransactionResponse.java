package dev.jbyanx.finsight.domain.transaction.dto;

import dev.jbyanx.finsight.domain.transaction.entity.Transaction.OriginType;
import dev.jbyanx.finsight.domain.transaction.entity.Transaction.TransactionStatus;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class TransactionResponse {

    private String id;
    private String description;
    private BigDecimal amount;
    private String category;
    private LocalDateTime date;
    private OriginType originType;
    private String originName;

    /** Nombre del bolsillo del que salió el dinero (null si no aplica). */
    private String pocketName;

    /** true = pagado con dinero de un tercero. NO es un gasto del usuario. */
    private boolean thirdParty;
    private String thirdPartyOwnerName;

    private boolean shared;
    private String sharedWithName;
    private BigDecimal sharedAmount;
    private LocalDateTime paymentDeadline;
    private Integer installments;
    private Integer currentInstallment;
    private TransactionStatus status;
    private LocalDateTime createdAt;
}