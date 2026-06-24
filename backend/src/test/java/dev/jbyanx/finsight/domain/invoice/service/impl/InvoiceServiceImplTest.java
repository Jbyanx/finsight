package dev.jbyanx.finsight.domain.invoice.service.impl;

import dev.jbyanx.finsight.domain.auth.entity.User;
import dev.jbyanx.finsight.domain.auth.repository.UserRepository;
import dev.jbyanx.finsight.domain.card.entity.CreditCard;
import dev.jbyanx.finsight.domain.card.repository.CreditCardRepository;
import dev.jbyanx.finsight.domain.invoice.dto.*;
import dev.jbyanx.finsight.domain.invoice.entity.Invoice;
import dev.jbyanx.finsight.domain.invoice.entity.Invoice.InvoiceFrequency;
import dev.jbyanx.finsight.domain.invoice.entity.InvoicePayment;
import dev.jbyanx.finsight.domain.invoice.entity.InvoicePayment.PaymentStatus;
import dev.jbyanx.finsight.domain.invoice.mapper.InvoiceMapper;
import dev.jbyanx.finsight.domain.invoice.repository.InvoicePaymentRepository;
import dev.jbyanx.finsight.domain.invoice.repository.InvoiceRepository;
import dev.jbyanx.finsight.shared.exception.ResourceNotFoundException;
import dev.jbyanx.finsight.shared.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceImplTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private InvoicePaymentRepository invoicePaymentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CreditCardRepository creditCardRepository;
    @Mock
    private InvoiceMapper invoiceMapper;

    @InjectMocks
    private InvoiceServiceImpl invoiceService;

    @Captor
    private ArgumentCaptor<Invoice> invoiceCaptor;
    @Captor
    private ArgumentCaptor<InvoicePayment> paymentCaptor;

    private User mockUser;
    private Invoice mockInvoice;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id("user-1").name("Jabes").build();
        mockInvoice = Invoice.builder()
                .id("inv-1")
                .user(mockUser)
                .description("Plan telefonía")
                .amount(new BigDecimal("50000"))
                .frequency(InvoiceFrequency.MONTHLY)
                .dueDay(15)
                .own(true)
                .active(true)
                .build();
    }

    // ---------- create ----------

    @Test
    void create_NoCard_Success() {
        InvoiceRequest req = baseInvoiceRequest();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(mockUser));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(mockInvoice);
        when(invoiceMapper.toResponse(mockInvoice)).thenReturn(InvoiceResponse.builder().build());

        assertNotNull(invoiceService.create("user-1", req));
        verify(creditCardRepository, never()).findByIdAndUserId(any(), any());
    }

    @Test
    void create_WithCard_ResolvesCard() {
        InvoiceRequest req = baseInvoiceRequest();
        ReflectionTestUtils.setField(req, "creditCardId", "card-1");
        CreditCard card = CreditCard.builder().id("card-1").user(mockUser).build();

        when(userRepository.findById("user-1")).thenReturn(Optional.of(mockUser));
        when(creditCardRepository.findByIdAndUserId("card-1", "user-1"))
                .thenReturn(Optional.of(card));
        when(invoiceRepository.save(any(Invoice.class))).thenReturn(mockInvoice);
        when(invoiceMapper.toResponse(any())).thenReturn(InvoiceResponse.builder().build());

        invoiceService.create("user-1", req);

        verify(invoiceRepository).save(invoiceCaptor.capture());
        assertEquals(card, invoiceCaptor.getValue().getCreditCard());
    }

    @Test
    void create_CardNotFound_ThrowsException() {
        InvoiceRequest req = baseInvoiceRequest();
        ReflectionTestUtils.setField(req, "creditCardId", "card-bad");
        when(userRepository.findById("user-1")).thenReturn(Optional.of(mockUser));
        when(creditCardRepository.findByIdAndUserId("card-bad", "user-1"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> invoiceService.create("user-1", req));
        verify(invoiceRepository, never()).save(any());
    }

    @Test
    void create_UserNotFound_ThrowsException() {
        when(userRepository.findById("user-bad")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> invoiceService.create("user-bad", baseInvoiceRequest()));
    }

    // ---------- update (regla: own=true anula responsable) ----------

    @Test
    void update_OwnTrue_NullsResponsibleName() {
        mockInvoice.setResponsibleName("Mamá");
        InvoiceRequest req = baseInvoiceRequest();
        ReflectionTestUtils.setField(req, "own", true);
        ReflectionTestUtils.setField(req, "responsibleName", "Mamá");

        when(invoiceRepository.findByIdAndUserId("inv-1", "user-1"))
                .thenReturn(Optional.of(mockInvoice));
        when(invoiceRepository.save(mockInvoice)).thenReturn(mockInvoice);
        when(invoiceMapper.toResponse(mockInvoice)).thenReturn(InvoiceResponse.builder().build());

        invoiceService.update("user-1", "inv-1", req);

        assertNull(mockInvoice.getResponsibleName());
    }

    @Test
    void update_OwnFalse_KeepsResponsibleName() {
        InvoiceRequest req = baseInvoiceRequest();
        ReflectionTestUtils.setField(req, "own", false);
        ReflectionTestUtils.setField(req, "responsibleName", "Mamá");

        when(invoiceRepository.findByIdAndUserId("inv-1", "user-1"))
                .thenReturn(Optional.of(mockInvoice));
        when(invoiceRepository.save(mockInvoice)).thenReturn(mockInvoice);
        when(invoiceMapper.toResponse(mockInvoice)).thenReturn(InvoiceResponse.builder().build());

        invoiceService.update("user-1", "inv-1", req);

        assertEquals("Mamá", mockInvoice.getResponsibleName());
    }

    // ---------- registerPayment ----------

    @Test
    void registerPayment_NoExisting_DefaultsAmountAndDate() {
        InvoicePaymentRequest req = paymentRequest(6, 2026);
        // amountPaid y paidAt quedan null → deben caer a invoice.amount y now()

        when(invoiceRepository.findByIdAndUserId("inv-1", "user-1"))
                .thenReturn(Optional.of(mockInvoice));
        when(invoicePaymentRepository
                .findByInvoiceIdAndPeriodMonthAndPeriodYear("inv-1", 6, 2026))
                .thenReturn(Optional.empty());
        when(invoicePaymentRepository.save(any(InvoicePayment.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(invoiceMapper.toPaymentResponse(any()))
                .thenReturn(InvoicePaymentResponse.builder().build());

        invoiceService.registerPayment("user-1", "inv-1", req);

        verify(invoicePaymentRepository).save(paymentCaptor.capture());
        InvoicePayment saved = paymentCaptor.getValue();
        assertEquals(new BigDecimal("50000"), saved.getAmountPaid()); // = invoice.amount
        assertNotNull(saved.getPaidAt());
        assertEquals(PaymentStatus.PAID, saved.getStatus());
    }

    @Test
    void registerPayment_ExplicitAmount_UsesIt() {
        InvoicePaymentRequest req = paymentRequest(6, 2026);
        ReflectionTestUtils.setField(req, "amountPaid", new BigDecimal("30000"));

        when(invoiceRepository.findByIdAndUserId("inv-1", "user-1"))
                .thenReturn(Optional.of(mockInvoice));
        when(invoicePaymentRepository
                .findByInvoiceIdAndPeriodMonthAndPeriodYear("inv-1", 6, 2026))
                .thenReturn(Optional.empty());
        when(invoicePaymentRepository.save(any(InvoicePayment.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(invoiceMapper.toPaymentResponse(any()))
                .thenReturn(InvoicePaymentResponse.builder().build());

        invoiceService.registerPayment("user-1", "inv-1", req);

        verify(invoicePaymentRepository).save(paymentCaptor.capture());
        assertEquals(new BigDecimal("30000"), paymentCaptor.getValue().getAmountPaid());
    }

    @Test
    void registerPayment_AlreadyPaidForPeriod_ThrowsException() {
        InvoicePayment existing = InvoicePayment.builder()
                .status(PaymentStatus.PAID).periodMonth(6).periodYear(2026).build();

        when(invoiceRepository.findByIdAndUserId("inv-1", "user-1"))
                .thenReturn(Optional.of(mockInvoice));
        when(invoicePaymentRepository
                .findByInvoiceIdAndPeriodMonthAndPeriodYear("inv-1", 6, 2026))
                .thenReturn(Optional.of(existing));

        assertThrows(UnauthorizedException.class,
                () -> invoiceService.registerPayment("user-1", "inv-1", paymentRequest(6, 2026)));
        verify(invoicePaymentRepository, never()).save(any());
    }

    @Test
    void registerPayment_ExistingNotPaid_Proceeds() {
        InvoicePayment existingPending = InvoicePayment.builder()
                .status(PaymentStatus.PENDING).periodMonth(6).periodYear(2026).build();

        when(invoiceRepository.findByIdAndUserId("inv-1", "user-1"))
                .thenReturn(Optional.of(mockInvoice));
        when(invoicePaymentRepository
                .findByInvoiceIdAndPeriodMonthAndPeriodYear("inv-1", 6, 2026))
                .thenReturn(Optional.of(existingPending));
        when(invoicePaymentRepository.save(any(InvoicePayment.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(invoiceMapper.toPaymentResponse(any()))
                .thenReturn(InvoicePaymentResponse.builder().build());

        invoiceService.registerPayment("user-1", "inv-1", paymentRequest(6, 2026));

        verify(invoicePaymentRepository).save(any(InvoicePayment.class));
    }

    // ---------- getById / delete / patch ----------

    @Test
    void getById_NotFound_ThrowsException() {
        when(invoiceRepository.findByIdAndUserId("inv-bad", "user-1"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> invoiceService.getById("user-1", "inv-bad"));
    }

    @Test
    void delete_SoftDelete_SetsInactive() {
        when(invoiceRepository.findByIdAndUserId("inv-1", "user-1"))
                .thenReturn(Optional.of(mockInvoice));

        invoiceService.delete("user-1", "inv-1");

        assertFalse(mockInvoice.isActive());
        verify(invoiceRepository).save(mockInvoice);
    }

    @Test
    void patch_OnlyProvidedFields_AreUpdated() {
        InvoicePatchRequest req = new InvoicePatchRequest();
        ReflectionTestUtils.setField(req, "description", "Plan nuevo");
        // amount, frequency, etc. quedan null → no deben tocarse

        when(invoiceRepository.findByIdAndUserId("inv-1", "user-1"))
                .thenReturn(Optional.of(mockInvoice));
        when(invoiceRepository.save(mockInvoice)).thenReturn(mockInvoice);
        when(invoiceMapper.toResponse(mockInvoice)).thenReturn(InvoiceResponse.builder().build());

        invoiceService.patch("user-1", "inv-1", req);

        assertEquals("Plan nuevo", mockInvoice.getDescription());
        assertEquals(new BigDecimal("50000"), mockInvoice.getAmount()); // intacto
    }

    // ---------- helpers ----------

    private InvoiceRequest baseInvoiceRequest() {
        InvoiceRequest req = new InvoiceRequest();
        ReflectionTestUtils.setField(req, "description", "Plan telefonía");
        ReflectionTestUtils.setField(req, "amount", new BigDecimal("50000"));
        ReflectionTestUtils.setField(req, "frequency", InvoiceFrequency.MONTHLY);
        ReflectionTestUtils.setField(req, "dueDay", 15);
        return req;
    }

    private InvoicePaymentRequest paymentRequest(int month, int year) {
        InvoicePaymentRequest req = new InvoicePaymentRequest();
        ReflectionTestUtils.setField(req, "periodMonth", month);
        ReflectionTestUtils.setField(req, "periodYear", year);
        return req;
    }
}