package dev.jbyanx.finsight.domain.invoice.dto;

import dev.jbyanx.finsight.domain.invoice.entity.InvoicePayment.PaymentStatus;
import dev.jbyanx.finsight.domain.invoice.entity.InvoicePayment.VoucherType;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class InvoicePaymentResponse {
    private String id;
    private BigDecimal amountPaid;
    private LocalDateTime paidAt;
    private Integer periodMonth;
    private Integer periodYear;
    private PaymentStatus status;
    private String notes;
    private String voucherUrl;
    private VoucherType voucherType;
    private LocalDateTime createdAt;
}