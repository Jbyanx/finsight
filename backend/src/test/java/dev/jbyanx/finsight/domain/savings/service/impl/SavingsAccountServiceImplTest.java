package dev.jbyanx.finsight.domain.savings.service.impl;

import dev.jbyanx.finsight.domain.auth.entity.User;
import dev.jbyanx.finsight.domain.auth.repository.UserRepository;
import dev.jbyanx.finsight.domain.savings.dto.*;
import dev.jbyanx.finsight.domain.savings.entity.Pocket;
import dev.jbyanx.finsight.domain.savings.entity.SavingsAccount;
import dev.jbyanx.finsight.domain.savings.mapper.SavingsAccountMapper;
import dev.jbyanx.finsight.domain.savings.repository.PocketRepository;
import dev.jbyanx.finsight.domain.savings.repository.SavingsAccountRepository;
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
class SavingsAccountServiceImplTest {

    @Mock
    private SavingsAccountRepository savingsAccountRepository;
    @Mock
    private PocketRepository pocketRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SavingsAccountMapper savingsAccountMapper;

    @InjectMocks
    private SavingsAccountServiceImpl savingsAccountService;

    @Captor
    private ArgumentCaptor<SavingsAccount> accountCaptor;
    @Captor
    private ArgumentCaptor<Pocket> pocketCaptor;

    private User mockUser;
    private SavingsAccount mockAccount;
    private Pocket mockPocket;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id("user-1").name("Jabes").build();

        mockAccount = SavingsAccount.builder()
                .id("acc-1")
                .user(mockUser)
                .institution("Nu")
                .nickname("Nu Cuenta")
                .balance(new BigDecimal("100000"))
                .shared(false)
                .active(true)
                .build();

        mockPocket = Pocket.builder()
                .id("pocket-1")
                .savingsAccount(mockAccount)
                .name("Matrícula U")
                .balance(new BigDecimal("100000"))
                .active(true)
                .build();
    }

    // ---------- create ----------

    @Test
    void create_NullBalance_DefaultsToZero() {
        SavingsAccountRequest req = new SavingsAccountRequest();
        ReflectionTestUtils.setField(req, "institution", "Nu");
        ReflectionTestUtils.setField(req, "balance", null);

        when(userRepository.findById("user-1")).thenReturn(Optional.of(mockUser));
        when(savingsAccountRepository.save(any(SavingsAccount.class))).thenReturn(mockAccount);
        when(savingsAccountMapper.toResponse(any())).thenReturn(SavingsAccountResponse.builder().build());

        savingsAccountService.create("user-1", req);

        verify(savingsAccountRepository).save(accountCaptor.capture());
        assertEquals(BigDecimal.ZERO, accountCaptor.getValue().getBalance());
    }

    @Test
    void create_UserNotFound_ThrowsException() {
        SavingsAccountRequest req = new SavingsAccountRequest();
        ReflectionTestUtils.setField(req, "institution", "Nu");
        when(userRepository.findById("user-bad")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> savingsAccountService.create("user-bad", req));
        verify(savingsAccountRepository, never()).save(any());
    }

    @Test
    void getById_NotFound_ThrowsException() {
        when(savingsAccountRepository.findByIdAndUserId("acc-bad", "user-1"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> savingsAccountService.getById("user-1", "acc-bad"));
    }

    // ---------- patch (reglas de negocio) ----------

    @Test
    void patch_SharedSetFalse_ClearsOwnerName() {
        mockAccount.setShared(true);
        mockAccount.setSharedOwnerName("Tía");

        SavingsAccountPatchRequest req = new SavingsAccountPatchRequest();
        ReflectionTestUtils.setField(req, "shared", Boolean.FALSE);
        // sharedOwnerName queda null a propósito: si lo mandáramos, se volvería a setear

        when(savingsAccountRepository.findByIdAndUserId("acc-1", "user-1"))
                .thenReturn(Optional.of(mockAccount));
        when(savingsAccountRepository.save(mockAccount)).thenReturn(mockAccount);
        when(savingsAccountMapper.toResponse(mockAccount))
                .thenReturn(SavingsAccountResponse.builder().build());

        savingsAccountService.patch("user-1", "acc-1", req);

        assertFalse(mockAccount.isShared());
        assertNull(mockAccount.getSharedOwnerName());
    }

    @Test
    void patch_SetOwnerName_Updates() {
        SavingsAccountPatchRequest req = new SavingsAccountPatchRequest();
        ReflectionTestUtils.setField(req, "sharedOwnerName", "Tía Rosa");

        when(savingsAccountRepository.findByIdAndUserId("acc-1", "user-1"))
                .thenReturn(Optional.of(mockAccount));
        when(savingsAccountRepository.save(mockAccount)).thenReturn(mockAccount);
        when(savingsAccountMapper.toResponse(mockAccount))
                .thenReturn(SavingsAccountResponse.builder().build());

        savingsAccountService.patch("user-1", "acc-1", req);

        assertEquals("Tía Rosa", mockAccount.getSharedOwnerName());
    }

    // ---------- patchPocket (reglas de negocio) ----------

    @Test
    void patchPocket_GoalLessThanBalance_ThrowsException() {
        PocketPatchRequest req = new PocketPatchRequest();
        ReflectionTestUtils.setField(req, "goalAmount", new BigDecimal("50000")); // < balance 100000

        when(savingsAccountRepository.findByIdAndUserId("acc-1", "user-1"))
                .thenReturn(Optional.of(mockAccount));
        when(pocketRepository.findByIdAndSavingsAccountId("pocket-1", "acc-1"))
                .thenReturn(Optional.of(mockPocket));

        assertThrows(UnauthorizedException.class,
                () -> savingsAccountService.patchPocket("user-1", "acc-1", "pocket-1", req));
        verify(pocketRepository, never()).save(any());
    }

    @Test
    void patchPocket_ValidGoal_Updates() {
        PocketPatchRequest req = new PocketPatchRequest();
        ReflectionTestUtils.setField(req, "goalAmount", new BigDecimal("200000"));

        when(savingsAccountRepository.findByIdAndUserId("acc-1", "user-1"))
                .thenReturn(Optional.of(mockAccount));
        when(pocketRepository.findByIdAndSavingsAccountId("pocket-1", "acc-1"))
                .thenReturn(Optional.of(mockPocket));
        when(pocketRepository.save(mockPocket)).thenReturn(mockPocket);
        when(savingsAccountMapper.toPocketResponse(mockPocket))
                .thenReturn(PocketResponse.builder().build());

        savingsAccountService.patchPocket("user-1", "acc-1", "pocket-1", req);

        assertEquals(new BigDecimal("200000"), mockPocket.getGoalAmount());
    }

    @Test
    void patchPocket_RemoveGoal_ClearsGoal() {
        mockPocket.setGoalAmount(new BigDecimal("500000"));
        PocketPatchRequest req = new PocketPatchRequest();
        ReflectionTestUtils.setField(req, "removeGoal", true);

        when(savingsAccountRepository.findByIdAndUserId("acc-1", "user-1"))
                .thenReturn(Optional.of(mockAccount));
        when(pocketRepository.findByIdAndSavingsAccountId("pocket-1", "acc-1"))
                .thenReturn(Optional.of(mockPocket));
        when(pocketRepository.save(mockPocket)).thenReturn(mockPocket);
        when(savingsAccountMapper.toPocketResponse(mockPocket))
                .thenReturn(PocketResponse.builder().build());

        savingsAccountService.patchPocket("user-1", "acc-1", "pocket-1", req);

        assertNull(mockPocket.getGoalAmount());
    }

    @Test
    void patchPocket_PocketNotFound_ThrowsException() {
        PocketPatchRequest req = new PocketPatchRequest();
        when(savingsAccountRepository.findByIdAndUserId("acc-1", "user-1"))
                .thenReturn(Optional.of(mockAccount));
        when(pocketRepository.findByIdAndSavingsAccountId("pocket-bad", "acc-1"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> savingsAccountService.patchPocket("user-1", "acc-1", "pocket-bad", req));
    }

    // ---------- createPocket / deletes ----------

    @Test
    void createPocket_NullBalance_DefaultsToZero() {
        PocketRequest req = new PocketRequest();
        ReflectionTestUtils.setField(req, "name", "Vacaciones");
        ReflectionTestUtils.setField(req, "balance", null);

        when(savingsAccountRepository.findByIdAndUserId("acc-1", "user-1"))
                .thenReturn(Optional.of(mockAccount));
        when(pocketRepository.save(any(Pocket.class))).thenReturn(mockPocket);
        when(savingsAccountMapper.toPocketResponse(any()))
                .thenReturn(PocketResponse.builder().build());

        savingsAccountService.createPocket("user-1", "acc-1", req);

        verify(pocketRepository).save(pocketCaptor.capture());
        assertEquals(BigDecimal.ZERO, pocketCaptor.getValue().getBalance());
    }

    @Test
    void delete_SoftDelete_SetsInactive() {
        when(savingsAccountRepository.findByIdAndUserId("acc-1", "user-1"))
                .thenReturn(Optional.of(mockAccount));

        savingsAccountService.delete("user-1", "acc-1");

        assertFalse(mockAccount.isActive());
        verify(savingsAccountRepository).save(mockAccount);
    }

    @Test
    void deletePocket_SoftDelete_SetsInactive() {
        when(savingsAccountRepository.findByIdAndUserId("acc-1", "user-1"))
                .thenReturn(Optional.of(mockAccount));
        when(pocketRepository.findByIdAndSavingsAccountId("pocket-1", "acc-1"))
                .thenReturn(Optional.of(mockPocket));

        savingsAccountService.deletePocket("user-1", "acc-1", "pocket-1");

        assertFalse(mockPocket.isActive());
        verify(pocketRepository).save(mockPocket);
    }
}