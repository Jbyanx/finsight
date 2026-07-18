package dev.jbyanx.finsight.domain.transaction.controller;

import dev.jbyanx.finsight.domain.auth.entity.User;
import dev.jbyanx.finsight.domain.auth.repository.UserRepository;
import dev.jbyanx.finsight.domain.auth.service.TokenService;
import dev.jbyanx.finsight.domain.card.entity.CreditCard;
import dev.jbyanx.finsight.domain.card.repository.CreditCardRepository;
import dev.jbyanx.finsight.domain.savings.entity.SavingsAccount;
import dev.jbyanx.finsight.domain.savings.repository.SavingsAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
class TransactionControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CreditCardRepository creditCardRepository;
    @Autowired private SavingsAccountRepository savingsAccountRepository;
    @Autowired private TokenService tokenService;

    private String accessToken;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .email("transaction-integration@finsight.dev")
                .name("Transaction Test User")
                .provider("google")
                .providerId("google-transaction-" + System.nanoTime())
                .build());

        accessToken = tokenService.createAccessToken(
                        testUser, testUser.getId(), "integration-test")
                .getToken();
    }

    @Test
    void dadoTarjetaConCupo_cuandoCrearTransaccion_entoncesRetorna201YActualizaUsedAmount()
            throws Exception {
        CreditCard card = creditCardRepository.save(CreditCard.builder()
                .user(testUser)
                .institution("Nu")
                .creditLimit(new BigDecimal("400000"))
                .usedAmount(BigDecimal.ZERO)
                .cutOffDay(3)
                .paymentDueDay(23)
                .build());

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "description": "Compra Temu",
                        "amount": 50000,
                        "category": "Compras",
                        "originType": "CREDIT_CARD",
                        "creditCardId": "%s",
                        "shared": false,
                        "installments": 1
                    }
                """.formatted(card.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Compra Temu"))
                .andExpect(jsonPath("$.originType").value("CREDIT_CARD"));

        CreditCard updated = creditCardRepository.findById(card.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(
                0, updated.getUsedAmount().compareTo(new BigDecimal("50000")));
    }

    @Test
    void dadoTransaccionQueExcedeCupo_cuandoCrearTransaccion_entoncesRetorna400()
            throws Exception {
        CreditCard card = creditCardRepository.save(CreditCard.builder()
                .user(testUser)
                .institution("Nu")
                .creditLimit(new BigDecimal("400000"))
                .usedAmount(new BigDecimal("390000"))
                .cutOffDay(3)
                .paymentDueDay(23)
                .build());

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "description": "Compra que excede cupo",
                        "amount": 20000,
                        "originType": "CREDIT_CARD",
                        "creditCardId": "%s",
                        "installments": 1
                    }
                """.formatted(card.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void dadoCuentaConSaldo_cuandoCrearTransaccion_entoncesDescuentaBalance()
            throws Exception {
        SavingsAccount account = savingsAccountRepository.save(SavingsAccount.builder()
                .user(testUser)
                .institution("Nu")
                .balance(new BigDecimal("500000"))
                .shared(false)
                .build());

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "description": "Retiro cajero",
                        "amount": 100000,
                        "originType": "SAVINGS",
                        "savingsAccountId": "%s",
                        "installments": 1
                    }
                """.formatted(account.getId())))
                .andExpect(status().isCreated());

        SavingsAccount updated = savingsAccountRepository
                .findById(account.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(
                0, updated.getBalance().compareTo(new BigDecimal("400000")));
    }

    @Test
    void dadoCuentaSinSaldoSuficiente_cuandoCrearTransaccion_entoncesRetorna400()
            throws Exception {
        SavingsAccount account = savingsAccountRepository.save(SavingsAccount.builder()
                .user(testUser)
                .institution("Nu")
                .balance(new BigDecimal("50000"))
                .shared(false)
                .build());

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "description": "Retiro mayor al saldo",
                        "amount": 100000,
                        "originType": "SAVINGS",
                        "savingsAccountId": "%s",
                        "installments": 1
                    }
                """.formatted(account.getId())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void dadoTransaccionCompartida_cuandoCrearTransaccion_entoncesPersisteDatos()
            throws Exception {
        CreditCard card = creditCardRepository.save(CreditCard.builder()
                .user(testUser)
                .institution("Rappi")
                .creditLimit(new BigDecimal("2600000"))
                .usedAmount(BigDecimal.ZERO)
                .cutOffDay(28)
                .paymentDueDay(10)
                .build());

        mockMvc.perform(post("/api/v1/transactions")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "description": "Celular papá",
                        "amount": 300000,
                        "originType": "CREDIT_CARD",
                        "creditCardId": "%s",
                        "shared": true,
                        "sharedWithName": "Papá",
                        "sharedAmount": 300000,
                        "installments": 12
                    }
                """.formatted(card.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shared").value(true))
                .andExpect(jsonPath("$.sharedWithName").value("Papá"))
                .andExpect(jsonPath("$.installments").value(12));
    }
}