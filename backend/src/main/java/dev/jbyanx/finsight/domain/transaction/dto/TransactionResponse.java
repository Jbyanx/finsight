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
    private boolean isShared;
    private String sharedWithName;
    private BigDecimal sharedAmount;
    private LocalDateTime paymentDeadline;
    private Integer installments;
    private Integer currentInstallment;
    private TransactionStatus status;
    private LocalDateTime createdAt;
}