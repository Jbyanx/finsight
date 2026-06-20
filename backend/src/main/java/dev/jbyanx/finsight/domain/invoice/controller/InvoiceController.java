package dev.jbyanx.finsight.domain.invoice.controller;

import dev.jbyanx.finsight.domain.invoice.dto.*;
import dev.jbyanx.finsight.domain.invoice.service.InvoiceService;
import dev.jbyanx.finsight.infrastructure.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    public ResponseEntity<InvoiceResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid InvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invoiceService.create(principal.getId(), request));
    }

    @PatchMapping("/{invoiceId}")
    public ResponseEntity<InvoiceResponse> patch(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String invoiceId,
            @RequestBody @Valid InvoicePatchRequest request) {
        return ResponseEntity.ok(
                invoiceService.patch(principal.getId(), invoiceId, request));
    }

    @GetMapping
    public ResponseEntity<List<InvoiceResponse>> getAll(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(invoiceService.getAllByUser(principal.getId()));
    }

    @GetMapping("/{invoiceId}")
    public ResponseEntity<InvoiceResponse> getById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String invoiceId) {
        return ResponseEntity.ok(invoiceService.getById(principal.getId(), invoiceId));
    }

    @PutMapping("/{invoiceId}")
    public ResponseEntity<InvoiceResponse> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String invoiceId,
            @RequestBody @Valid InvoiceRequest request) {
        return ResponseEntity.ok(
                invoiceService.update(principal.getId(), invoiceId, request));
    }

    @DeleteMapping("/{invoiceId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String invoiceId) {
        invoiceService.delete(principal.getId(), invoiceId);
        return ResponseEntity.noContent().build();
    }

    //historial general de facturas
    @GetMapping("/payments")
    public ResponseEntity<Page<InvoicePaymentResponse>> getAllPayments(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(invoiceService.getAllPayments(
                principal.getId(),
                PageRequest.of(page, size)));
    }
    // --- Pagos de facturas ---

    @PostMapping("/{invoiceId}/payments")
    public ResponseEntity<InvoicePaymentResponse> registerPayment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String invoiceId,
            @RequestBody @Valid InvoicePaymentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invoiceService.registerPayment(
                        principal.getId(), invoiceId, request));
    }

    @GetMapping("/{invoiceId}/payments")
    public ResponseEntity<Page<InvoicePaymentResponse>> getPaymentHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String invoiceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(invoiceService.getPaymentHistory(
                principal.getId(), invoiceId,
                PageRequest.of(page, size,
                        Sort.by("periodYear", "periodMonth").descending())));
    }
}