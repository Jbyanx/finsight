package dev.jbyanx.finsight.domain.invoice.dto;

import dev.jbyanx.finsight.domain.invoice.entity.Invoice.InvoiceFrequency;
import dev.jbyanx.finsight.domain.invoice.entity.Invoice.PaymentMethod;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class InvoiceResponse {
    private String id;
    private String description;
    private BigDecimal amount;
    private InvoiceFrequency frequency;
    private Integer dueDay;
    private PaymentMethod paymentMethod;
    private String creditCardName;
    private boolean isOwn;
    private String responsibleName;
    private String payerNote;
    private LocalDateTime createdAt;
}