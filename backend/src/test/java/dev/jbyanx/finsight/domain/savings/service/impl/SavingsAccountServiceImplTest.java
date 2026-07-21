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
import dev.jbyanx.finsight.shared.exception.BusinessRuleException;
import dev.jbyanx.finsight.shared.exception.InsufficientFundsException;
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

        assertThrows(BusinessRuleException.class,
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

    // ---------- updatePocket / PUT (misma regla que patchPocket) ----------

    @Test
    void updatePocket_GoalLessThanBalance_ThrowsException_PocketUntouched() {
        // Bolsillo con saldo 100000; PUT intenta fijar meta 50000
        PocketRequest req = new PocketRequest();
        ReflectionTestUtils.setField(req, "name", "Renombrado");
        ReflectionTestUtils.setField(req, "goalAmount", new BigDecimal("50000"));

        when(savingsAccountRepository.findByIdAndUserId("acc-1", "user-1"))
                .thenReturn(Optional.of(mockAccount));
        when(pocketRepository.findByIdAndSavingsAccountId("pocket-1", "acc-1"))
                .thenReturn(Optional.of(mockPocket));

        assertThrows(BusinessRuleException.class,
                () -> savingsAccountService.updatePocket("user-1", "acc-1", "pocket-1", req));

        // El bolsillo queda intacto: la validacion corre antes de tocar la meta
        assertNull(mockPocket.getGoalAmount());
        verify(pocketRepository, never()).save(any());
    }

    @Test
    void updatePocket_ValidGoal_Updates() {
        PocketRequest req = new PocketRequest();
        ReflectionTestUtils.setField(req, "name", "Matricula U");
        ReflectionTestUtils.setField(req, "goalAmount", new BigDecimal("200000"));

        when(savingsAccountRepository.findByIdAndUserId("acc-1", "user-1"))
                .thenReturn(Optional.of(mockAccount));
        when(pocketRepository.findByIdAndSavingsAccountId("pocket-1", "acc-1"))
                .thenReturn(Optional.of(mockPocket));
        when(pocketRepository.save(mockPocket)).thenReturn(mockPocket);
        when(savingsAccountMapper.toPocketResponse(mockPocket))
                .thenReturn(PocketResponse.builder().build());

        savingsAccountService.updatePocket("user-1", "acc-1", "pocket-1", req);

        assertEquals(new BigDecimal("200000"), mockPocket.getGoalAmount());
        verify(pocketRepository).save(mockPocket);
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

    // ---------- createPocket (validación de disponible) ----------

    @Test
    void createPocket_ExceedsAvailable_ThrowsException() {
        // Cuenta $100.000 sin bolsillos → disponible $100.000
        PocketRequest req = new PocketRequest();
        ReflectionTestUtils.setField(req, "name", "Viaje");
        ReflectionTestUtils.setField(req, "balance", new BigDecimal("300000")); // > disponible

        when(savingsAccountRepository.findByIdAndUserId("acc-1", "user-1"))
                .thenReturn(Optional.of(mockAccount));

        assertThrows(InsufficientFundsException.class,
                () -> savingsAccountService.createPocket("user-1", "acc-1", req));
        verify(pocketRepository, never()).save(any());
    }

    // ---------- deposit / withdraw ----------

    @Test
    void depositToPocket_ExceedsAvailable_ThrowsException() {
        // Cuenta $1.000.000 con bolsillo de $900.000 → disponible $100.000
        SavingsAccount account = accountWithBalance("1000000");
        Pocket pocket = pocketInAccount(account, "900000");
        PocketAmountRequest req = amountRequest("200000"); // > disponible 100.000

        when(savingsAccountRepository.findByIdAndUserId("acc-1", "user-1"))
                .thenReturn(Optional.of(account));
        when(pocketRepository.findByIdAndSavingsAccountId("pocket-1", "acc-1"))
                .thenReturn(Optional.of(pocket));

        assertThrows(InsufficientFundsException.class,
                () -> savingsAccountService.depositToPocket("user-1", "acc-1", "pocket-1", req));
        verify(pocketRepository, never()).save(any());
    }

    @Test
    void withdrawFromPocket_ExceedsPocketBalance_ThrowsException() {
        SavingsAccount account = accountWithBalance("1000000");
        Pocket pocket = pocketInAccount(account, "200000");
        PocketAmountRequest req = amountRequest("300000"); // > saldo del bolsillo

        when(savingsAccountRepository.findByIdAndUserId("acc-1", "user-1"))
                .thenReturn(Optional.of(account));
        when(pocketRepository.findByIdAndSavingsAccountId("pocket-1", "acc-1"))
                .thenReturn(Optional.of(pocket));

        assertThrows(InsufficientFundsException.class,
                () -> savingsAccountService.withdrawFromPocket("user-1", "acc-1", "pocket-1", req));
        verify(pocketRepository, never()).save(any());
    }

    @Test
    void depositToPocket_Happy_AdjustsAvailable_BalanceUnchanged() {
        // Cuenta $1.000.000, bolsillo $200.000 → disponible $800.000
        SavingsAccount account = accountWithBalance("1000000");
        Pocket pocket = pocketInAccount(account, "200000");
        PocketAmountRequest req = amountRequest("300000");

        when(savingsAccountRepository.findByIdAndUserId("acc-1", "user-1"))
                .thenReturn(Optional.of(account));
        when(pocketRepository.findByIdAndSavingsAccountId("pocket-1", "acc-1"))
                .thenReturn(Optional.of(pocket));
        when(pocketRepository.save(pocket)).thenReturn(pocket);
        when(savingsAccountMapper.toPocketResponse(pocket))
                .thenReturn(PocketResponse.builder().build());

        savingsAccountService.depositToPocket("user-1", "acc-1", "pocket-1", req);

        assertEquals(0, new BigDecimal("500000").compareTo(pocket.getBalance()));
        // El total de la cuenta NO cambia; solo se movió plata a "reservado".
        assertEquals(0, new BigDecimal("1000000").compareTo(account.getBalance()));
        assertEquals(0, new BigDecimal("500000").compareTo(account.getAvailableBalance()));
    }

    @Test
    void withdrawFromPocket_Happy_AdjustsAvailable_BalanceUnchanged() {
        // Cuenta $1.000.000, bolsillo $500.000 → disponible $500.000
        SavingsAccount account = accountWithBalance("1000000");
        Pocket pocket = pocketInAccount(account, "500000");
        PocketAmountRequest req = amountRequest("100000");

        when(savingsAccountRepository.findByIdAndUserId("acc-1", "user-1"))
                .thenReturn(Optional.of(account));
        when(pocketRepository.findByIdAndSavingsAccountId("pocket-1", "acc-1"))
                .thenReturn(Optional.of(pocket));
        when(pocketRepository.save(pocket)).thenReturn(pocket);
        when(savingsAccountMapper.toPocketResponse(pocket))
                .thenReturn(PocketResponse.builder().build());

        savingsAccountService.withdrawFromPocket("user-1", "acc-1", "pocket-1", req);

        assertEquals(0, new BigDecimal("400000").compareTo(pocket.getBalance()));
        assertEquals(0, new BigDecimal("1000000").compareTo(account.getBalance()));
        assertEquals(0, new BigDecimal("600000").compareTo(account.getAvailableBalance()));
    }

    @Test
    void pocketLifecycle_AvailableEqualsBalanceMinusPockets_AtEveryStep() {
        // Cuenta $1.000.000, sin bolsillos → disponible = balance
        SavingsAccount account = accountWithBalance("1000000");
        assertEquals(0, account.getBalance().compareTo(account.getAvailableBalance()));

        // Crear bolsillo "Viaje" con $200.000
        PocketRequest createReq = new PocketRequest();
        ReflectionTestUtils.setField(createReq, "name", "Viaje");
        ReflectionTestUtils.setField(createReq, "balance", new BigDecimal("200000"));

        when(savingsAccountRepository.findByIdAndUserId("acc-1", "user-1"))
                .thenReturn(Optional.of(account));
        when(pocketRepository.save(any(Pocket.class))).thenAnswer(inv -> inv.getArgument(0));
        when(savingsAccountMapper.toPocketResponse(any()))
                .thenReturn(PocketResponse.builder().build());

        savingsAccountService.createPocket("user-1", "acc-1", createReq);

        // El bolsillo persistido se refleja en la cuenta (como haría JPA:
        // @PrePersist marca active=true, algo que el mock no ejecuta).
        Pocket viaje = pocketCaptorValue();
        viaje.setActive(true);
        account.getPockets().add(viaje);
        assertAvailableConsistent(account);
        assertEquals(0, new BigDecimal("800000").compareTo(account.getAvailableBalance()));

        // Depositar $300.000 → reservado 500.000, disponible 500.000
        when(pocketRepository.findByIdAndSavingsAccountId(viaje.getId(), "acc-1"))
                .thenReturn(Optional.of(viaje));
        savingsAccountService.depositToPocket("user-1", "acc-1", viaje.getId(),
                amountRequest("300000"));
        assertAvailableConsistent(account);
        assertEquals(0, new BigDecimal("500000").compareTo(account.getAvailableBalance()));

        // Retirar $150.000 → reservado 350.000, disponible 650.000
        savingsAccountService.withdrawFromPocket("user-1", "acc-1", viaje.getId(),
                amountRequest("150000"));
        assertAvailableConsistent(account);
        assertEquals(0, new BigDecimal("650000").compareTo(account.getAvailableBalance()));

        // El total de la cuenta nunca cambió al operar bolsillos
        assertEquals(0, new BigDecimal("1000000").compareTo(account.getBalance()));
    }

    // ---------- helpers ----------

    private SavingsAccount accountWithBalance(String balance) {
        return SavingsAccount.builder()
                .id("acc-1")
                .user(mockUser)
                .institution("Nu")
                .balance(new BigDecimal(balance))
                .shared(false)
                .active(true)
                .build();
    }

    private Pocket pocketInAccount(SavingsAccount account, String balance) {
        Pocket pocket = Pocket.builder()
                .id("pocket-1")
                .savingsAccount(account)
                .name("Viaje")
                .balance(new BigDecimal(balance))
                .active(true)
                .build();
        account.getPockets().add(pocket);
        return pocket;
    }

    private PocketAmountRequest amountRequest(String amount) {
        PocketAmountRequest req = new PocketAmountRequest();
        ReflectionTestUtils.setField(req, "amount", new BigDecimal(amount));
        return req;
    }

    private Pocket pocketCaptorValue() {
        verify(pocketRepository, atLeastOnce()).save(pocketCaptor.capture());
        return pocketCaptor.getValue();
    }

    private void assertAvailableConsistent(SavingsAccount account) {
        BigDecimal pocketsTotal = account.getPockets().stream()
                .filter(Pocket::isActive)
                .map(Pocket::getBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0,
                account.getBalance().subtract(pocketsTotal)
                        .compareTo(account.getAvailableBalance()));
    }
}