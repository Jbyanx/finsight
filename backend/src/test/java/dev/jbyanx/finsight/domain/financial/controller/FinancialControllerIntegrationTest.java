package dev.jbyanx.finsight.domain.financial.controller;

import dev.jbyanx.finsight.domain.auth.entity.User;
import dev.jbyanx.finsight.domain.auth.repository.UserRepository;
import dev.jbyanx.finsight.domain.auth.service.TokenService;
import dev.jbyanx.finsight.domain.card.entity.CreditCard;
import dev.jbyanx.finsight.domain.card.repository.CreditCardRepository;
import dev.jbyanx.finsight.domain.cash.entity.Cash;
import dev.jbyanx.finsight.domain.cash.repository.CashRepository;
import dev.jbyanx.finsight.domain.debt.entity.FixedDebt;
import dev.jbyanx.finsight.domain.debt.repository.FixedDebtRepository;
import dev.jbyanx.finsight.domain.savings.entity.SavingsAccount;
import dev.jbyanx.finsight.domain.savings.repository.SavingsAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class FinancialControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CreditCardRepository creditCardRepository;
    @Autowired private SavingsAccountRepository savingsAccountRepository;
    @Autowired private CashRepository cashRepository;
    @Autowired private FixedDebtRepository fixedDebtRepository;
    @Autowired private TokenService tokenService;

    private String accessToken;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .email("financial-integration@finsight.dev")
                .name("Financial Test User")
                .provider("google")
                .providerId("google-financial-" + System.nanoTime())
                .build());

        accessToken = tokenService.createAccessToken(
                        testUser, testUser.getId(), "integration-test")
                .getToken();
    }

    @Test
    void dadoUsuarioSinDatos_cuandoConsultarSummary_entoncesTodoEnCero() throws Exception {
        mockMvc.perform(get("/api/v1/financial/summary")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCreditLimit").value(0))
                .andExpect(jsonPath("$.totalUsedAmount").value(0))
                .andExpect(jsonPath("$.cardAlerts").isEmpty())
                .andExpect(jsonPath("$.upcomingInvoices").isEmpty());
    }

    @Test
    void dadoTarjetaEnRojo_cuandoConsultarSummary_entoncesApareceEnCardAlerts()
            throws Exception {
        creditCardRepository.save(CreditCard.builder()
                .user(testUser)
                .institution("Nu")
                .creditLimit(new BigDecimal("400000"))
                .usedAmount(new BigDecimal("130000")) // 32.5% -> RED
                .cutOffDay(3)
                .paymentDueDay(23)
                .build());

        mockMvc.perform(get("/api/v1/financial/summary")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cardAlerts", org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.cardAlerts[0].status").value("RED"))
                .andExpect(jsonPath("$.overallUtilizationPercentage").value(32.5));
    }

    @Test
    void dadoCuentaCompartida_cuandoConsultarSummary_entoncesNoSumaAlTotal()
            throws Exception {
        savingsAccountRepository.save(SavingsAccount.builder()
                .user(testUser)
                .institution("Nu")
                .balance(new BigDecimal("500000"))
                .shared(false)
                .build());

        savingsAccountRepository.save(SavingsAccount.builder()
                .user(testUser)
                .institution("Rappi")
                .balance(new BigDecimal("200000"))
                .shared(true)
                .sharedOwnerName("Tía")
                .build());

        mockMvc.perform(get("/api/v1/financial/summary")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSavingsBalance").value(500000));
    }

    @Test
    void dadoEfectivoYCuenta_cuandoConsultarSummary_entoncesSumaBalanceDisponible()
            throws Exception {
        cashRepository.save(Cash.builder()
                .user(testUser)
                .balance(new BigDecimal("100000"))
                .build());

        savingsAccountRepository.save(SavingsAccount.builder()
                .user(testUser)
                .institution("Nu")
                .balance(new BigDecimal("300000"))
                .shared(false)
                .build());

        mockMvc.perform(get("/api/v1/financial/summary")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCashBalance").value(100000))
                .andExpect(jsonPath("$.totalSavingsBalance").value(300000))
                .andExpect(jsonPath("$.totalAvailableBalance").value(400000));
    }

    @Test
    void dadoDeudaActiva_cuandoConsultarSummary_entoncesSumaDeudaPendiente()
            throws Exception {
        fixedDebtRepository.save(FixedDebt.builder()
                .user(testUser)
                .institution("Banco de Bogotá")
                .totalAmount(new BigDecimal("8000000"))
                .monthlyPayment(new BigDecimal("141889"))
                .paymentDueDay(16)
                .remainingBalance(new BigDecimal("141889"))
                .build());

        mockMvc.perform(get("/api/v1/financial/summary")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRemainingDebt").value(141889))
                .andExpect(jsonPath("$.totalMonthlyDebtPayment").value(141889));
    }

    @Test
    void dadoSinToken_cuandoConsultarSummary_entoncesRetorna401() throws Exception {
        mockMvc.perform(get("/api/v1/financial/summary"))
                .andExpect(status().isUnauthorized());
    }
}