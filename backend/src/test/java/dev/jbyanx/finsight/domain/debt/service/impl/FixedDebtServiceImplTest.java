package dev.jbyanx.finsight.domain.debt.service.impl;

import dev.jbyanx.finsight.domain.auth.entity.User;
import dev.jbyanx.finsight.domain.auth.repository.UserRepository;
import dev.jbyanx.finsight.domain.debt.dto.DebtPaymentRequest;
import dev.jbyanx.finsight.domain.debt.dto.FixedDebtPatchRequest;
import dev.jbyanx.finsight.domain.debt.dto.FixedDebtRequest;
import dev.jbyanx.finsight.domain.debt.dto.FixedDebtResponse;
import dev.jbyanx.finsight.domain.debt.entity.FixedDebt;
import dev.jbyanx.finsight.domain.debt.mapper.FixedDebtMapper;
import dev.jbyanx.finsight.domain.debt.repository.FixedDebtRepository;
import dev.jbyanx.finsight.shared.exception.DebtAlreadyPaidOffException;
import dev.jbyanx.finsight.shared.exception.PaymentExceedsBalanceException;
import dev.jbyanx.finsight.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FixedDebtServiceImplTest {

    @Mock
    private FixedDebtRepository fixedDebtRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FixedDebtMapper fixedDebtMapper;

    @InjectMocks
    private FixedDebtServiceImpl fixedDebtService;

    private User mockUser;
    private FixedDebt mockDebt;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id("user-1").build();

        mockDebt = FixedDebt.builder()
                .id("debt-1")
                .user(mockUser)
                .institution("Banco de Bogotá")
                .description("Moto")
                .totalAmount(new BigDecimal("3000000"))
                .remainingBalance(new BigDecimal("141889"))
                .monthlyPayment(new BigDecimal("150000"))
                .paymentDueDay(16)
                .active(true)
                .build();
    }

    @Test
    void create_Success() {
        FixedDebtRequest request = FixedDebtRequest.builder()
                .institution("Banco de Bogotá")
                .totalAmount(new BigDecimal("3000000"))
                .build();

        when(userRepository.findById("user-1")).thenReturn(Optional.of(mockUser));
        when(fixedDebtRepository.save(any(FixedDebt.class))).thenReturn(mockDebt);
        when(fixedDebtMapper.toResponse(mockDebt)).thenReturn(FixedDebtResponse.builder().build());

        assertNotNull(fixedDebtService.create("user-1", request));
        verify(fixedDebtRepository).save(any(FixedDebt.class));
    }

    @Test
    void patch_Success() {
        FixedDebtPatchRequest request = FixedDebtPatchRequest.builder()
                .paymentDueDay(20)
                .build();

        when(fixedDebtRepository.findByIdAndUserId("debt-1", "user-1"))
                .thenReturn(Optional.of(mockDebt));
        when(fixedDebtRepository.save(any())).thenReturn(mockDebt);

        fixedDebtService.patch("user-1", "debt-1", request);

        assertEquals(20, mockDebt.getPaymentDueDay());
        verify(fixedDebtRepository).save(mockDebt);
    }

    @Test
    void applyPayment_Success_ReducesBalance() {
        DebtPaymentRequest payment = DebtPaymentRequest.builder()
                .amount(new BigDecimal("41889"))
                .build();

        when(fixedDebtRepository.findByIdAndUserId("debt-1", "user-1"))
                .thenReturn(Optional.of(mockDebt));
        when(fixedDebtRepository.save(any())).thenReturn(mockDebt);

        fixedDebtService.applyPayment("user-1", "debt-1", payment);

        // El balance original era 141889. Menos 41889 = 100000
        assertEquals(0, new BigDecimal("100000").compareTo(mockDebt.getRemainingBalance()));
        assertTrue(mockDebt.isActive());
        verify(fixedDebtRepository).save(mockDebt);
    }

    @Test
    void applyPayment_ExactAmount_PaysOffAndDeactivates() {
        DebtPaymentRequest payment = new DebtPaymentRequest(new BigDecimal("141889"));

        when(fixedDebtRepository.findByIdAndUserId("debt-1", "user-1"))
                .thenReturn(Optional.of(mockDebt));
        when(fixedDebtRepository.save(any())).thenReturn(mockDebt);

        fixedDebtService.applyPayment("user-1", "debt-1", payment);

        assertEquals(0, BigDecimal.ZERO.compareTo(mockDebt.getRemainingBalance()));
        assertFalse(mockDebt.isActive()); // La entidad debe desactivarse automáticamente
        verify(fixedDebtRepository).save(mockDebt);
    }

    @Test
    void applyPayment_ExceedsBalance_ThrowsException() {
        DebtPaymentRequest payment = new DebtPaymentRequest(new BigDecimal("200000"));

        when(fixedDebtRepository.findByIdAndUserId("debt-1", "user-1"))
                .thenReturn(Optional.of(mockDebt));

        assertThrows(PaymentExceedsBalanceException.class,
                () -> fixedDebtService.applyPayment("user-1", "debt-1", payment));

        verify(fixedDebtRepository, never()).save(any());
    }

    @Test
    void applyPayment_AlreadyPaidOff_ThrowsException() {
        // Configuramos la deuda como ya saldada
        mockDebt.setRemainingBalance(BigDecimal.ZERO);
        DebtPaymentRequest payment = new DebtPaymentRequest(new BigDecimal("10000"));

        when(fixedDebtRepository.findByIdAndUserId("debt-1", "user-1"))
                .thenReturn(Optional.of(mockDebt));

        assertThrows(DebtAlreadyPaidOffException.class,
                () -> fixedDebtService.applyPayment("user-1", "debt-1", payment));

        verify(fixedDebtRepository, never()).save(any());
    }

    @Test
    void delete_SoftDelete_Success() {
        when(fixedDebtRepository.findByIdAndUserId("debt-1", "user-1"))
                .thenReturn(Optional.of(mockDebt));

        fixedDebtService.delete("user-1", "debt-1");

        assertFalse(mockDebt.isActive());
        verify(fixedDebtRepository).save(mockDebt);
    }
}