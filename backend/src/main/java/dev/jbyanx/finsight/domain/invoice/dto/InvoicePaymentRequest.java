package dev.jbyanx.finsight.domain.invoice.dto;

import dev.jbyanx.finsight.domain.invoice.entity.InvoicePayment.PaymentStatus;
import dev.jbyanx.finsight.domain.invoice.entity.InvoicePayment.VoucherType;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class InvoicePaymentRequest {

    @DecimalMin(value = "0.01")
    private BigDecimal amountPaid;

    private LocalDateTime paidAt;

    @NotNull(message = "Period month is required")
    @Min(1) @Max(12)
    private Integer periodMonth;

    @NotNull(message = "Period year is required")
    private Integer periodYear;

    private String notes;
    private String voucherUrl;
    private VoucherType voucherType;
}