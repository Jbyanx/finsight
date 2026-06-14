package dev.jbyanx.finsight.domain.invoice.dto;

import dev.jbyanx.finsight.domain.invoice.entity.Invoice.InvoiceFrequency;
import dev.jbyanx.finsight.domain.invoice.entity.Invoice.PaymentMethod;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class InvoiceRequest {

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotNull(message = "Frequency is required")
    private InvoiceFrequency frequency;

    @NotNull(message = "Due day is required")
    @Min(value = 1) @Max(value = 31)
    private Integer dueDay;

    private PaymentMethod paymentMethod;
    private String creditCardId;

    private boolean isOwn = true;
    private String responsibleName;
    private String payerNote;
}