package dev.jbyanx.finsight.domain.invoice.service;

import dev.jbyanx.finsight.domain.invoice.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface InvoiceService {

    InvoiceResponse create(String userId, InvoiceRequest request);

    List<InvoiceResponse> getAllByUser(String userId);

    InvoiceResponse getById(String userId, String invoiceId);

    InvoiceResponse update(String userId, String invoiceId, InvoiceRequest request);

    void delete(String userId, String invoiceId);

    // Pagos de facturas
    InvoicePaymentResponse registerPayment(String userId, String invoiceId,
                                           InvoicePaymentRequest request);

    Page<InvoicePaymentResponse> getPaymentHistory(String userId, String invoiceId,
                                                   Pageable pageable);

    Page<InvoicePaymentResponse> getAllPayments(String userId, Pageable pageable);

    InvoiceResponse patch(String userId, String invoiceId, InvoicePatchRequest request);
}