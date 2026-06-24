package dev.jbyanx.finsight.domain.income.service.impl;

import dev.jbyanx.finsight.domain.auth.entity.User;
import dev.jbyanx.finsight.domain.auth.repository.UserRepository;
import dev.jbyanx.finsight.domain.cash.entity.Cash;
import dev.jbyanx.finsight.domain.cash.repository.CashRepository;
import dev.jbyanx.finsight.domain.income.dto.*;
import dev.jbyanx.finsight.domain.income.entity.IncomeEntry;
import dev.jbyanx.finsight.domain.income.entity.IncomeEntry.DestinationType;
import dev.jbyanx.finsight.domain.income.entity.IncomeSource;
import dev.jbyanx.finsight.domain.income.entity.IncomeSource.IncomeFrequency;
import dev.jbyanx.finsight.domain.income.entity.IncomeSource.IncomeType;
import dev.jbyanx.finsight.domain.income.entity.OperationalExpense;
import dev.jbyanx.finsight.domain.income.mapper.IncomeMapper;
import dev.jbyanx.finsight.domain.income.repository.IncomeEntryRepository;
import dev.jbyanx.finsight.domain.income.repository.IncomeSourceRepository;
import dev.jbyanx.finsight.domain.income.repository.OperationalExpenseRepository;
import dev.jbyanx.finsight.domain.savings.entity.Pocket;
import dev.jbyanx.finsight.domain.savings.entity.SavingsAccount;
import dev.jbyanx.finsight.domain.savings.repository.PocketRepository;
import dev.jbyanx.finsight.domain.savings.repository.SavingsAccountRepository;
import dev.jbyanx.finsight.shared.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IncomeServiceImplTest {

    @Mock private IncomeSourceRepository incomeSourceRepository;
    @Mock private IncomeEntryRepository incomeEntryRepository;
    @Mock private OperationalExpenseRepository operationalExpenseRepository;
    @Mock private UserRepository userRepository;
    @Mock private CashRepository cashRepository;
    @Mock private SavingsAccountRepository savingsAccountRepository;
    @Mock private PocketRepository pocketRepository;
    @Mock private IncomeMapper incomeMapper;

    @InjectMocks
    private IncomeServiceImpl incomeService;

    @Captor
    private ArgumentCaptor<IncomeEntry> entryCaptor;

    private User mockUser;
    private IncomeSource mockSource;

    @BeforeEach
    void setUp() {
        mockUser = User.builder().id("user-1").name("Jabes").build();
        mockSource = IncomeSource.builder()
                .id("src-1").user(mockUser)
                .description("Turno hotel")
                .type(IncomeType.VARIABLE)
                .active(true)
                .build();
    }

    // ---------- fuentes de ingreso ----------

    @Test
    void createSource_Success() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(mockUser));
        when(incomeSourceRepository.save(any(IncomeSource.class))).thenReturn(mockSource);
        when(incomeMapper.toSourceResponse(mockSource))
                .thenReturn(IncomeSourceResponse.builder().build());

        assertNotNull(incomeService.createSource("user-1", sourceRequest()));
        verify(incomeSourceRepository).save(any(IncomeSource.class));
    }

    @Test
    void createSource_UserNotFound_ThrowsException() {
        when(userRepository.findById("user-bad")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> incomeService.createSource("user-bad", sourceRequest()));
        verify(incomeSourceRepository, never()).save(any());
    }

    @Test
    void updateSource_NotFound_ThrowsException() {
        when(incomeSourceRepository.findByIdAndUserId("src-bad", "user-1"))
                .thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> incomeService.updateSource("user-1", "src-bad", sourceRequest()));
    }

    @Test
    void deleteSource_SoftDelete_SetsInactive() {
        when(incomeSourceRepository.findByIdAndUserId("src-1", "user-1"))
                .thenReturn(Optional.of(mockSource));

        incomeService.deleteSource("user-1", "src-1");

        assertFalse(mockSource.isActive());
        verify(incomeSourceRepository).save(mockSource);
    }

    @Test
    void patchSource_OnlyProvidedFields_AreUpdated() {
        IncomeSourcePatchRequest req = new IncomeSourcePatchRequest();
        ReflectionTestUtils.setField(req, "description", "Turno noche");
        // type queda null → no debe cambiar

        when(incomeSourceRepository.findByIdAndUserId("src-1", "user-1"))
                .thenReturn(Optional.of(mockSource));
        when(incomeSourceRepository.save(mockSource)).thenReturn(mockSource);
        when(incomeMapper.toSourceResponse(mockSource))
                .thenReturn(IncomeSourceResponse.builder().build());

        incomeService.patchSource("user-1", "src-1", req);

        assertEquals("Turno noche", mockSource.getDescription());
        assertEquals(IncomeType.VARIABLE, mockSource.getType()); // intacto
    }

    // ---------- registerIncome: el switch de destino ----------

    @Test
    void registerIncome_Cash_ExistingCash_AddsToBalance() {
        Cash cash = Cash.builder().user(mockUser).balance(new BigDecimal("100000")).build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(mockUser));
        when(cashRepository.findByUserId("user-1")).thenReturn(Optional.of(cash));
        when(incomeEntryRepository.save(any(IncomeEntry.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(incomeMapper.toEntryResponse(any()))
                .thenReturn(IncomeEntryResponse.builder().build());

        incomeService.registerIncome("user-1",
                entryRequest(new BigDecimal("60000"), DestinationType.CASH));

        assertEquals(new BigDecimal("160000"), cash.getBalance());
        verify(cashRepository).save(cash);
    }

    @Test
    void registerIncome_Cash_NoCash_CreatesIt() {
        Cash created = Cash.builder().user(mockUser).balance(BigDecimal.ZERO).build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(mockUser));
        when(cashRepository.findByUserId("user-1")).thenReturn(Optional.empty());
        when(cashRepository.save(any(Cash.class))).thenReturn(created);
        when(incomeEntryRepository.save(any(IncomeEntry.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(incomeMapper.toEntryResponse(any()))
                .thenReturn(IncomeEntryResponse.builder().build());

        incomeService.registerIncome("user-1",
                entryRequest(new BigDecimal("60000"), DestinationType.CASH));

        assertEquals(new BigDecimal("60000"), created.getBalance());
        verify(cashRepository, times(2)).save(any(Cash.class)); // crear + actualizar
    }

    @Test
    void registerIncome_Savings_AddsToAccountBalance() {
        SavingsAccount account = SavingsAccount.builder()
                .id("acc-1").user(mockUser).balance(new BigDecimal("100000")).build();
        IncomeEntryRequest req = entryRequest(new BigDecimal("50000"), DestinationType.SAVINGS);
        ReflectionTestUtils.setField(req, "savingsAccountId", "acc-1");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(mockUser));
        when(savingsAccountRepository.findByIdAndUserId("acc-1", "user-1"))
                .thenReturn(Optional.of(account));
        when(incomeEntryRepository.save(any(IncomeEntry.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(incomeMapper.toEntryResponse(any()))
                .thenReturn(IncomeEntryResponse.builder().build());

        incomeService.registerIncome("user-1", req);

        assertEquals(new BigDecimal("150000"), account.getBalance());
        verify(savingsAccountRepository).save(account);
    }

    @Test
    void registerIncome_Savings_AccountNotFound_ThrowsException() {
        IncomeEntryRequest req = entryRequest(new BigDecimal("50000"), DestinationType.SAVINGS);
        ReflectionTestUtils.setField(req, "savingsAccountId", "acc-bad");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(mockUser));
        when(savingsAccountRepository.findByIdAndUserId("acc-bad", "user-1"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> incomeService.registerIncome("user-1", req));
        verify(incomeEntryRepository, never()).save(any());
    }

    @Test
    void registerIncome_Pocket_AddsToPocketAndParentAccount() {
        SavingsAccount account = SavingsAccount.builder()
                .id("acc-1").user(mockUser).balance(new BigDecimal("100000")).build();
        Pocket pocket = Pocket.builder()
                .id("p-1").savingsAccount(account)
                .name("Matrícula U").balance(new BigDecimal("5000")).build();

        IncomeEntryRequest req = entryRequest(new BigDecimal("20000"), DestinationType.POCKET);
        ReflectionTestUtils.setField(req, "pocketId", "p-1");
        ReflectionTestUtils.setField(req, "savingsAccountId", "acc-1");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(mockUser));
        when(pocketRepository.findByIdAndSavingsAccountId("p-1", "acc-1"))
                .thenReturn(Optional.of(pocket));
        when(incomeEntryRepository.save(any(IncomeEntry.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(incomeMapper.toEntryResponse(any()))
                .thenReturn(IncomeEntryResponse.builder().build());

        incomeService.registerIncome("user-1", req);

        assertEquals(new BigDecimal("25000"), pocket.getBalance());   // bolsillo
        assertEquals(new BigDecimal("120000"), account.getBalance()); // cuenta padre
        verify(pocketRepository).save(pocket);
        verify(savingsAccountRepository).save(account);
    }

    @Test
    void registerIncome_Pocket_NotFound_ThrowsException() {
        IncomeEntryRequest req = entryRequest(new BigDecimal("20000"), DestinationType.POCKET);
        ReflectionTestUtils.setField(req, "pocketId", "p-bad");
        ReflectionTestUtils.setField(req, "savingsAccountId", "acc-1");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(mockUser));
        when(pocketRepository.findByIdAndSavingsAccountId("p-bad", "acc-1"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> incomeService.registerIncome("user-1", req));
    }

    @Test
    void registerIncome_WithSource_ResolvesAndLinksIt() {
        Cash cash = Cash.builder().user(mockUser).balance(BigDecimal.ZERO).build();
        IncomeEntryRequest req = entryRequest(new BigDecimal("60000"), DestinationType.CASH);
        ReflectionTestUtils.setField(req, "incomeSourceId", "src-1");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(mockUser));
        when(incomeSourceRepository.findByIdAndUserId("src-1", "user-1"))
                .thenReturn(Optional.of(mockSource));
        when(cashRepository.findByUserId("user-1")).thenReturn(Optional.of(cash));
        when(incomeEntryRepository.save(any(IncomeEntry.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(incomeMapper.toEntryResponse(any()))
                .thenReturn(IncomeEntryResponse.builder().build());

        incomeService.registerIncome("user-1", req);

        verify(incomeEntryRepository).save(entryCaptor.capture());
        assertEquals(mockSource, entryCaptor.getValue().getIncomeSource());
    }

    @Test
    void registerIncome_SourceNotFound_ThrowsException() {
        IncomeEntryRequest req = entryRequest(new BigDecimal("60000"), DestinationType.CASH);
        ReflectionTestUtils.setField(req, "incomeSourceId", "src-bad");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(mockUser));
        when(incomeSourceRepository.findByIdAndUserId("src-bad", "user-1"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> incomeService.registerIncome("user-1", req));
        verify(incomeEntryRepository, never()).save(any());
    }

    @Test
    void registerIncome_UserNotFound_ThrowsException() {
        when(userRepository.findById("user-bad")).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> incomeService.registerIncome("user-bad",
                        entryRequest(new BigDecimal("60000"), DestinationType.CASH)));
    }

    // ---------- gastos operativos ----------

    @Test
    void createExpense_WithSource_Success() {
        OperationalExpenseRequest req = expenseRequest();
        ReflectionTestUtils.setField(req, "incomeSourceId", "src-1");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(mockUser));
        when(incomeSourceRepository.findByIdAndUserId("src-1", "user-1"))
                .thenReturn(Optional.of(mockSource));
        when(operationalExpenseRepository.save(any(OperationalExpense.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(incomeMapper.toExpenseResponse(any()))
                .thenReturn(OperationalExpenseResponse.builder().build());

        assertNotNull(incomeService.createExpense("user-1", req));
        verify(operationalExpenseRepository).save(any(OperationalExpense.class));
    }

    @Test
    void createExpense_SourceNotFound_ThrowsException() {
        OperationalExpenseRequest req = expenseRequest();
        ReflectionTestUtils.setField(req, "incomeSourceId", "src-bad");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(mockUser));
        when(incomeSourceRepository.findByIdAndUserId("src-bad", "user-1"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> incomeService.createExpense("user-1", req));
        verify(operationalExpenseRepository, never()).save(any());
    }

    @Test
    void deleteExpense_SoftDelete_SetsInactive() {
        OperationalExpense expense = OperationalExpense.builder()
                .id("exp-1").user(mockUser).description("Gasolina")
                .estimatedAmount(new BigDecimal("20000"))
                .frequency(IncomeFrequency.PER_SHIFT).active(true).build();
        when(operationalExpenseRepository.findByIdAndUserId("exp-1", "user-1"))
                .thenReturn(Optional.of(expense));

        incomeService.deleteExpense("user-1", "exp-1");

        assertFalse(expense.isActive());
        verify(operationalExpenseRepository).save(expense);
    }

    // ---------- listados de fuentes / gastos ----------

    @Test
    void getAllSources_ReturnsMappedList() {
        when(incomeSourceRepository.findAllByUserIdAndActiveTrue("user-1"))
                .thenReturn(List.of(mockSource));
        when(incomeMapper.toSourceResponseList(List.of(mockSource)))
                .thenReturn(List.of(IncomeSourceResponse.builder().build()));

        assertEquals(1, incomeService.getAllSources("user-1").size());
        verify(incomeSourceRepository).findAllByUserIdAndActiveTrue("user-1");
    }

    @Test
    void getAllExpenses_ReturnsMappedList() {
        OperationalExpense expense = OperationalExpense.builder()
                .id("exp-1").user(mockUser).description("Gasolina")
                .estimatedAmount(new BigDecimal("20000"))
                .frequency(IncomeFrequency.PER_SHIFT).active(true).build();

        when(operationalExpenseRepository.findAllByUserIdAndActiveTrue("user-1"))
                .thenReturn(List.of(expense));
        when(incomeMapper.toExpenseResponseList(List.of(expense)))
                .thenReturn(List.of(OperationalExpenseResponse.builder().build()));

        assertEquals(1, incomeService.getAllExpenses("user-1").size());
        verify(operationalExpenseRepository).findAllByUserIdAndActiveTrue("user-1");
    }

    // ---------- historial paginado ----------

    @Test
    void getIncomeHistory_ReturnsMappedPage() {
        IncomeEntry entry = IncomeEntry.builder()
                .id("e-1").user(mockUser)
                .amount(new BigDecimal("60000"))
                .destinationType(DestinationType.CASH).build();
        Pageable pageable = PageRequest.of(0, 10);
        Page<IncomeEntry> page = new PageImpl<>(List.of(entry), pageable, 1);

        when(incomeEntryRepository.findAllByUserIdOrderByReceivedAtDesc("user-1", pageable))
                .thenReturn(page);
        when(incomeMapper.toEntryResponse(entry))
                .thenReturn(IncomeEntryResponse.builder().build());

        Page<IncomeEntryResponse> result = incomeService.getIncomeHistory("user-1", pageable);

        assertEquals(1, result.getTotalElements());
        verify(incomeEntryRepository)
                .findAllByUserIdOrderByReceivedAtDesc("user-1", pageable);
    }

    // ---------- updateSource (reemplazo completo) ----------

    @Test
    void updateSource_Success_OverwritesFields() {
        IncomeSourceRequest req = new IncomeSourceRequest();
        ReflectionTestUtils.setField(req, "description", "Mesada");
        ReflectionTestUtils.setField(req, "type", IncomeType.FIXED);
        ReflectionTestUtils.setField(req, "estimatedAmount", new BigDecimal("200000"));
        ReflectionTestUtils.setField(req, "frequency", IncomeFrequency.MONTHLY);

        when(incomeSourceRepository.findByIdAndUserId("src-1", "user-1"))
                .thenReturn(Optional.of(mockSource));
        when(incomeSourceRepository.save(mockSource)).thenReturn(mockSource);
        when(incomeMapper.toSourceResponse(mockSource))
                .thenReturn(IncomeSourceResponse.builder().build());

        incomeService.updateSource("user-1", "src-1", req);

        assertEquals("Mesada", mockSource.getDescription());
        assertEquals(IncomeType.FIXED, mockSource.getType());
        assertEquals(new BigDecimal("200000"), mockSource.getEstimatedAmount());
        assertEquals(IncomeFrequency.MONTHLY, mockSource.getFrequency());
    }

    // ---------- updateExpense (reemplazo completo) ----------

    @Test
    void updateExpense_Success_OverwritesFields() {
        OperationalExpense expense = OperationalExpense.builder()
                .id("exp-1").user(mockUser).description("Gasolina")
                .estimatedAmount(new BigDecimal("20000"))
                .frequency(IncomeFrequency.PER_SHIFT).active(true).build();

        OperationalExpenseRequest req = new OperationalExpenseRequest();
        ReflectionTestUtils.setField(req, "description", "Merienda");
        ReflectionTestUtils.setField(req, "frequency", IncomeFrequency.WEEKLY);
        ReflectionTestUtils.setField(req, "estimatedAmount", new BigDecimal("3000"));

        when(operationalExpenseRepository.findByIdAndUserId("exp-1", "user-1"))
                .thenReturn(Optional.of(expense));
        when(operationalExpenseRepository.save(expense)).thenReturn(expense);
        when(incomeMapper.toExpenseResponse(expense))
                .thenReturn(OperationalExpenseResponse.builder().build());

        incomeService.updateExpense("user-1", "exp-1", req);

        assertEquals("Merienda", expense.getDescription());
        assertEquals(IncomeFrequency.WEEKLY, expense.getFrequency());
        assertEquals(new BigDecimal("3000"), expense.getEstimatedAmount());
    }

    @Test
    void updateExpense_NotFound_ThrowsException() {
        when(operationalExpenseRepository.findByIdAndUserId("exp-bad", "user-1"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> incomeService.updateExpense("user-1", "exp-bad", expenseRequest()));
        verify(operationalExpenseRepository, never()).save(any());
    }

    // ---------- patchExpense (parcial) ----------

    @Test
    void patchExpense_OnlyProvidedFields_AreUpdated() {
        OperationalExpense expense = OperationalExpense.builder()
                .id("exp-1").user(mockUser).description("Gasolina")
                .estimatedAmount(new BigDecimal("20000"))
                .frequency(IncomeFrequency.PER_SHIFT).active(true).build();

        OperationalExpensePatchRequest req = new OperationalExpensePatchRequest();
        ReflectionTestUtils.setField(req, "estimatedAmount", new BigDecimal("25000"));
        // description y frequency quedan null → no deben cambiar

        when(operationalExpenseRepository.findByIdAndUserId("exp-1", "user-1"))
                .thenReturn(Optional.of(expense));
        when(operationalExpenseRepository.save(expense)).thenReturn(expense);
        when(incomeMapper.toExpenseResponse(expense))
                .thenReturn(OperationalExpenseResponse.builder().build());

        incomeService.patchExpense("user-1", "exp-1", req);

        assertEquals(new BigDecimal("25000"), expense.getEstimatedAmount());
        assertEquals("Gasolina", expense.getDescription());          // intacto
        assertEquals(IncomeFrequency.PER_SHIFT, expense.getFrequency()); // intacto
    }

    @Test
    void patchExpense_NotFound_ThrowsException() {
        when(operationalExpenseRepository.findByIdAndUserId("exp-bad", "user-1"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> incomeService.patchExpense("user-1", "exp-bad",
                        new OperationalExpensePatchRequest()));
    }

    // ---------- helpers ----------

    private IncomeSourceRequest sourceRequest() {
        IncomeSourceRequest req = new IncomeSourceRequest();
        ReflectionTestUtils.setField(req, "description", "Turno hotel");
        ReflectionTestUtils.setField(req, "type", IncomeType.VARIABLE);
        return req;
    }

    private IncomeEntryRequest entryRequest(BigDecimal amount, DestinationType destination) {
        IncomeEntryRequest req = new IncomeEntryRequest();
        ReflectionTestUtils.setField(req, "amount", amount);
        ReflectionTestUtils.setField(req, "destinationType", destination);
        return req;
    }

    private OperationalExpenseRequest expenseRequest() {
        OperationalExpenseRequest req = new OperationalExpenseRequest();
        ReflectionTestUtils.setField(req, "description", "Gasolina NKD");
        ReflectionTestUtils.setField(req, "frequency", IncomeFrequency.PER_SHIFT);
        ReflectionTestUtils.setField(req, "estimatedAmount", new BigDecimal("20000"));
        return req;
    }
}