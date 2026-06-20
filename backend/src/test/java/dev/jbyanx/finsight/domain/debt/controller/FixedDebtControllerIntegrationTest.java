package dev.jbyanx.finsight.domain.debt.controller;

import dev.jbyanx.finsight.domain.auth.entity.User;
import dev.jbyanx.finsight.domain.auth.repository.UserRepository;
import dev.jbyanx.finsight.domain.auth.service.TokenService;
import dev.jbyanx.finsight.domain.debt.entity.FixedDebt;
import dev.jbyanx.finsight.domain.debt.repository.FixedDebtRepository;
import dev.jbyanx.finsight.infrastructure.security.JwtService;
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
class FixedDebtControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private FixedDebtRepository fixedDebtRepository;
    @Autowired private TokenService tokenService;


    private String accessToken;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .email("debt-integration@finsight.dev")
                .name("Debt Test User")
                .provider("google")
                .providerId("google-debt-" + System.nanoTime())
                .build());

        accessToken = tokenService.createAccessToken(
                        testUser, testUser.getId(), "integration-test")
                .getToken();
    }

    @Test
    void dadoUsuarioAutenticado_cuandoCrearDeuda_entoncesRetorna201() throws Exception {
        mockMvc.perform(post("/api/v1/debts")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                    {
                        "institution": "Banco de Bogotá",
                        "description": "Crédito moto",
                        "totalAmount": 8000000,
                        "monthlyPayment": 141889,
                        "paymentDueDay": 16,
                        "remainingBalance": 141889
                    }
                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.institution").value("Banco de Bogotá"))
                .andExpect(jsonPath("$.paidOff").value(false))
                .andExpect(jsonPath("$.progressPercentage").value(98.23));
    }

    @Test
    void dadoPagoQueSaldaDeuda_cuandoAplicarPago_entoncesQuedaPagada() throws Exception {
        FixedDebt debt = fixedDebtRepository.save(FixedDebt.builder()
                .user(testUser)
                .institution("Banco de Bogotá")
                .totalAmount(new BigDecimal("141889"))
                .monthlyPayment(new BigDecimal("141889"))
                .paymentDueDay(16)
                .remainingBalance(new BigDecimal("141889"))
                .build());

        mockMvc.perform(post("/api/v1/debts/" + debt.getId() + "/payments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 141889}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paidOff").value(true))
                .andExpect(jsonPath("$.remainingBalance").value(0));
    }

    @Test
    void dadoPagoQueExcedeBalance_cuandoAplicarPago_entoncesRetorna400() throws Exception {
        FixedDebt debt = fixedDebtRepository.save(FixedDebt.builder()
                .user(testUser)
                .institution("Banco de Bogotá")
                .totalAmount(new BigDecimal("141889"))
                .monthlyPayment(new BigDecimal("141889"))
                .paymentDueDay(16)
                .remainingBalance(new BigDecimal("141889"))
                .build());

        mockMvc.perform(post("/api/v1/debts/" + debt.getId() + "/payments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 200000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Payment Exceeds Balance"));
    }

    @Test
    void dadoDeudaYaSaldada_cuandoAplicarOtroPago_entoncesRetorna409() throws Exception {
        FixedDebt debt = fixedDebtRepository.save(FixedDebt.builder()
                .user(testUser)
                .institution("Banco de Bogotá")
                .totalAmount(new BigDecimal("141889"))
                .monthlyPayment(new BigDecimal("141889"))
                .paymentDueDay(16)
                .remainingBalance(BigDecimal.ZERO)
                .active(false)
                .build());

        mockMvc.perform(post("/api/v1/debts/" + debt.getId() + "/payments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 1000}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Debt Already Paid Off"));
    }
}