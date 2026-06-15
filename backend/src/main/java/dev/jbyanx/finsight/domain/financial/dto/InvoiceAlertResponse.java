package dev.jbyanx.finsight.domain.financial.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;

@Getter
@Builder
public class InvoiceAlertResponse {
    private String invoiceId;
    private String description;
    private BigDecimal amount;
    private Integer dueDay;
    private boolean own;
    private String responsibleName;
}