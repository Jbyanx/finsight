package dev.jbyanx.finsight.domain.invoice.service.impl;

import dev.jbyanx.finsight.domain.auth.entity.User;
import dev.jbyanx.finsight.domain.auth.repository.UserRepository;
import dev.jbyanx.finsight.domain.card.entity.CreditCard;
import dev.jbyanx.finsight.domain.card.repository.CreditCardRepository;
import dev.jbyanx.finsight.domain.invoice.dto.*;
import dev.jbyanx.finsight.domain.invoice.entity.Invoice;
import dev.jbyanx.finsight.domain.invoice.entity.InvoicePayment;
import dev.jbyanx.finsight.domain.invoice.entity.InvoicePayment.PaymentStatus;
import dev.jbyanx.finsight.domain.invoice.mapper.InvoiceMapper;
import dev.jbyanx.finsight.domain.invoice.repository.InvoicePaymentRepository;
import dev.jbyanx.finsight.domain.invoice.repository.InvoiceRepository;
import dev.jbyanx.finsight.domain.invoice.service.InvoiceService;
import dev.jbyanx.finsight.shared.exception.InvoiceAlreadyPaidException;
import dev.jbyanx.finsight.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoicePaymentRepository invoicePaymentRepository;
    private final UserRepository userRepository;
    private final CreditCardRepository creditCardRepository;
    private final InvoiceMapper invoiceMapper;

    @Override
    @Transactional
    public InvoiceResponse create(String userId, InvoiceRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        CreditCard creditCard = null;
        if (request.getCreditCardId() != null) {
            creditCard = creditCardRepository
                    .findByIdAndUserId(request.getCreditCardId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "CreditCard", request.getCreditCardId()));
        }

        Invoice invoice = Invoice.builder()
                .user(user)
                .description(request.getDescription())
                .amount(request.getAmount())
                .frequency(request.getFrequency())
                .dueDay(request.getDueDay())
                .paymentMethod(request.getPaymentMethod())
                .creditCard(creditCard)
                .own(request.isOwn())
                .responsibleName(request.getResponsibleName())
                .payerNote(request.getPayerNote())
                .build();

        return invoiceMapper.toResponse(invoiceRepository.save(invoice));
    }

    @Override
    public List<InvoiceResponse> getAllByUser(String userId) {
        return invoiceMapper.toResponseList(
                invoiceRepository.findAllByUserIdAndActiveTrue(userId));
    }

    @Override
    public InvoiceResponse getById(String userId, String invoiceId) {
        return invoiceMapper.toResponse(findInvoiceBelongingToUser(userId, invoiceId));
    }

    @Override
    @Transactional
    public InvoiceResponse update(String userId, String invoiceId, InvoiceRequest request) {
        Invoice invoice = findInvoiceBelongingToUser(userId, invoiceId);

        CreditCard creditCard = null;
        if (request.getCreditCardId() != null) {
            creditCard = creditCardRepository
                    .findByIdAndUserId(request.getCreditCardId(), userId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "CreditCard", request.getCreditCardId()));
        }

        invoice.setDescription(request.getDescription());
        invoice.setAmount(request.getAmount());
        invoice.setFrequency(request.getFrequency());
        invoice.setDueDay(request.getDueDay());
        invoice.setPaymentMethod(request.getPaymentMethod());
        invoice.setCreditCard(creditCard);
        invoice.setOwn(request.isOwn());
        invoice.setResponsibleName(request.isOwn() ? null : request.getResponsibleName());
        invoice.setPayerNote(request.getPayerNote());

        return invoiceMapper.toResponse(invoiceRepository.save(invoice));
    }

    @Override
    @Transactional
    public void delete(String userId, String invoiceId) {
        Invoice invoice = findInvoiceBelongingToUser(userId, invoiceId);
        invoice.setActive(false);
        invoiceRepository.save(invoice);
    }

    @Override
    @Transactional
    public InvoicePaymentResponse registerPayment(String userId, String invoiceId,
                                                  InvoicePaymentRequest request) {
        Invoice invoice = findInvoiceBelongingToUser(userId, invoiceId);

        // Verifica que no existe ya un pago para ese período
        invoicePaymentRepository
                .findByInvoiceIdAndPeriodMonthAndPeriodYear(
                        invoiceId, request.getPeriodMonth(), request.getPeriodYear())
                .ifPresent(existing -> {
                    if (existing.getStatus() == PaymentStatus.PAID) {
                        throw new InvoiceAlreadyPaidException(
                                request.getPeriodMonth(), request.getPeriodYear());
                    }
                });

        InvoicePayment payment = InvoicePayment.builder()
                .invoice(invoice)
                .amountPaid(request.getAmountPaid() != null
                        ? request.getAmountPaid() : invoice.getAmount())
                .paidAt(request.getPaidAt() != null
                        ? request.getPaidAt() : LocalDateTime.now())
                .periodMonth(request.getPeriodMonth())
                .periodYear(request.getPeriodYear())
                .status(PaymentStatus.PAID)
                .notes(request.getNotes())
                .voucherUrl(request.getVoucherUrl())
                .voucherType(request.getVoucherType())
                .build();

        return invoiceMapper.toPaymentResponse(invoicePaymentRepository.save(payment));
    }

    @Override
    public Page<InvoicePaymentResponse> getPaymentHistory(String userId, String invoiceId,
                                                          Pageable pageable) {
        findInvoiceBelongingToUser(userId, invoiceId);
        return invoicePaymentRepository
                .findAllByInvoiceIdOrderByPeriodYearDescPeriodMonthDesc(invoiceId, pageable)
                .map(invoiceMapper::toPaymentResponse);
    }

    private Invoice findInvoiceBelongingToUser(String userId, String invoiceId) {
        return invoiceRepository.findByIdAndUserId(invoiceId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));
    }

    @Override
    public Page<InvoicePaymentResponse> getAllPayments(String userId, Pageable pageable) {
        return invoicePaymentRepository
                .findAllByUserIdOrderByPeriodDesc(userId, pageable)
                .map(invoiceMapper::toPaymentResponse);
    }

    @Override
    @Transactional
    public InvoiceResponse patch(String userId, String invoiceId, InvoicePatchRequest request) {
        Invoice invoice = findInvoiceBelongingToUser(userId, invoiceId);

        if (request.getDescription() != null) invoice.setDescription(request.getDescription());
        if (request.getAmount() != null) invoice.setAmount(request.getAmount());
        if (request.getFrequency() != null) invoice.setFrequency(request.getFrequency());
        if (request.getDueDay() != null) invoice.setDueDay(request.getDueDay());
        if (request.getPaymentMethod() != null) invoice.setPaymentMethod(request.getPaymentMethod());
        if (request.getPayerNote() != null) invoice.setPayerNote(request.getPayerNote());

        return invoiceMapper.toResponse(invoiceRepository.save(invoice));
    }
}