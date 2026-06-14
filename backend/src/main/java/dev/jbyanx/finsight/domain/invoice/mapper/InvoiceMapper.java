package dev.jbyanx.finsight.domain.invoice.mapper;

import dev.jbyanx.finsight.domain.invoice.dto.InvoicePaymentResponse;
import dev.jbyanx.finsight.domain.invoice.dto.InvoiceResponse;
import dev.jbyanx.finsight.domain.invoice.entity.Invoice;
import dev.jbyanx.finsight.domain.invoice.entity.InvoicePayment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import java.util.List;

@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    @Mapping(target = "creditCardName",
            expression = "java(invoice.getCreditCard() != null " +
                    "? (invoice.getCreditCard().getNickname() != null " +
                    "? invoice.getCreditCard().getNickname() " +
                    ": invoice.getCreditCard().getInstitution()) : null)")
    InvoiceResponse toResponse(Invoice invoice);

    List<InvoiceResponse> toResponseList(List<Invoice> invoices);

    InvoicePaymentResponse toPaymentResponse(InvoicePayment payment);

    List<InvoicePaymentResponse> toPaymentResponseList(List<InvoicePayment> payments);
}