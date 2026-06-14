package dev.jbyanx.finsight.domain.invoice.dto;

import dev.jbyanx.finsight.domain.invoice.entity.Invoice.InvoiceFrequency;
import dev.jbyanx.finsight.domain.invoice.entity.Invoice.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class InvoicePatchRequest {

    // Todos opcionales — solo se actualizan los que vienen
    private String description;

    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    private InvoiceFrequency frequency;

    @Min(1) @Max(31)
    private Integer dueDay;

    private PaymentMethod paymentMethod;
    private String payerNote;
}