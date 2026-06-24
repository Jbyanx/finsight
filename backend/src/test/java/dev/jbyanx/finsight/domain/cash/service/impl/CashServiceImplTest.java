package dev.jbyanx.finsight.domain.cash.service.impl;

import dev.jbyanx.finsight.domain.auth.entity.User;
import dev.jbyanx.finsight.domain.auth.repository.UserRepository;
import dev.jbyanx.finsight.domain.cash.dto.CashOperation;
import dev.jbyanx.finsight.domain.cash.dto.CashResponse;
import dev.jbyanx.finsight.domain.cash.dto.CashUpdateRequest;
import dev.jbyanx.finsight.domain.cash.entity.Cash;
import dev.jbyanx.finsight.domain.cash.mapper.CashMapper;
import dev.jbyanx.finsight.domain.cash.repository.CashRepository;
import dev.jbyanx.finsight.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class CashServiceImplTest {

    @Mock
    private CashRepository cashRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CashMapper cashMapper;

    @InjectMocks
    private CashServiceImpl cashService;

    private User mockUser;
    private Cash mockCash;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id("user-1").name("Jabes").build();
        mockCash = Cash.builder()
                .id("cash-1")
                .user(mockUser)
                .balance(new BigDecimal("100000"))
                .build();
    }

    // ---------- getByUser ----------

    @Test
    void getByUser_CashExists_ReturnsResponse() {
        when(cashRepository.findByUserId("user-1")).thenReturn(Optional.of(mockCash));
        when(cashMapper.toResponse(mockCash)).thenReturn(CashResponse.builder().build());

        assertNotNull(cashService.getByUser("user-1"));
        verify(cashRepository, never()).save(any());
    }

    @Test
    void getByUser_CashNotExists_CreatesIt() {
        Cash created = Cash.builder().user(mockUser).balance(BigDecimal.ZERO).build();
        when(cashRepository.findByUserId("user-1")).thenReturn(Optional.empty());
        when(userRepository.findById("user-1")).thenReturn(Optional.of(mockUser));
        when(cashRepository.save(any(Cash.class))).thenReturn(created);
        when(cashMapper.toResponse(created)).thenReturn(CashResponse.builder().build());

        assertNotNull(cashService.getByUser("user-1"));
        verify(cashRepository).save(any(Cash.class));
    }

    @Test
    void getByUser_CashNotExists_UserNotFound_ThrowsException() {
        when(cashRepository.findByUserId("user-bad")).thenReturn(Optional.empty());
        when(userRepository.findById("user-bad")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> cashService.getByUser("user-bad"));
        verify(cashRepository, never()).save(any());
    }

    // ---------- update ----------

    @Test
    void update_Add_IncreasesBalance() {
        when(cashRepository.findByUserId("user-1")).thenReturn(Optional.of(mockCash));
        when(cashRepository.save(mockCash)).thenReturn(mockCash);
        when(cashMapper.toResponse(mockCash)).thenReturn(CashResponse.builder().build());

        cashService.update("user-1", request(new BigDecimal("50000"), CashOperation.ADD));

        assertEquals(new BigDecimal("150000"), mockCash.getBalance());
        verify(cashRepository).save(mockCash);
    }

    @Test
    void update_Subtract_DecreasesBalance() {
        when(cashRepository.findByUserId("user-1")).thenReturn(Optional.of(mockCash));
        when(cashRepository.save(mockCash)).thenReturn(mockCash);
        when(cashMapper.toResponse(mockCash)).thenReturn(CashResponse.builder().build());

        cashService.update("user-1", request(new BigDecimal("30000"), CashOperation.SUBTRACT));

        assertEquals(new BigDecimal("70000"), mockCash.getBalance());
        verify(cashRepository).save(mockCash);
    }

    @Test
    void update_Subtract_InsufficientBalance_ThrowsException() {
        mockCash.setBalance(new BigDecimal("20000"));
        when(cashRepository.findByUserId("user-1")).thenReturn(Optional.of(mockCash));

        assertThrows(IllegalArgumentException.class,
                () -> cashService.update("user-1",
                        request(new BigDecimal("50000"), CashOperation.SUBTRACT)));

        verify(cashRepository, never()).save(any());
    }

    private CashUpdateRequest request(BigDecimal amount, CashOperation operation) {
        CashUpdateRequest req = new CashUpdateRequest();
        ReflectionTestUtils.setField(req, "amount", amount);
        ReflectionTestUtils.setField(req, "operation", operation);
        return req;
    }
}