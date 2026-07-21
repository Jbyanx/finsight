package dev.jbyanx.finsight.domain.card.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.jbyanx.finsight.domain.auth.entity.User;
import dev.jbyanx.finsight.domain.auth.repository.UserRepository;
import dev.jbyanx.finsight.domain.auth.service.TokenService;
import dev.jbyanx.finsight.domain.card.entity.CreditCard;
import dev.jbyanx.finsight.domain.card.repository.CreditCardRepository;
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
import static org.hamcrest.Matchers.hasSize;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CreditCardControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private CreditCardRepository creditCardRepository;
    @Autowired private JwtService jwtService;
    @Autowired private TokenService tokenService;

    private String accessToken;
    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = userRepository.save(User.builder()
                .email("integration@finsight.dev")
                .name("Integration User")
                .provider("google")
                .providerId("google-integration-" + System.nanoTime())
                .build());

        // Genera Y persiste el token — el filtro valida contra BD
        accessToken = tokenService.createAccessToken(
                        testUser, testUser.getId(), "integration-test")
                .getToken();
    }

    @Test
    void dadoUsuarioAutenticado_cuandoCrearTarjeta_entoncesRetorna201() throws Exception {
        mockMvc.perform(post("/api/v1/cards")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "institution": "Nu",
                    "nickname": "Nu Principal",
                    "creditLimit": 400000,
                    "cutOffDay": 3,
                    "paymentDueDay": 23
                }
            """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.institution").value("Nu"))
                .andExpect(jsonPath("$.utilizationStatus").value("SAFE"))
                .andExpect(jsonPath("$.usedAmount").value(0));
    }

    @Test
    void dadoUsuarioAutenticado_cuandoListarTarjetas_entoncesRetorna200() throws Exception {
        creditCardRepository.save(CreditCard.builder()
                .user(testUser)
                .institution("Rappi")
                .creditLimit(new BigDecimal("2600000"))
                .usedAmount(BigDecimal.ZERO)
                .cutOffDay(28)
                .paymentDueDay(10)
                .build());

        mockMvc.perform(get("/api/v1/cards")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].institution").value("Rappi"));
    }

    @Test
    void dadoSinToken_cuandoListarTarjetas_entoncesRetorna401() throws Exception {
        mockMvc.perform(get("/api/v1/cards"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dadoCupoMenorAlUtilizado_cuandoPatchTarjeta_entoncesRetorna400() throws Exception {
        // Regresión: esta regla de negocio caía en el fallback del handler y salía
        // como 500 genérico, perdiendo el mensaje que el frontend muestra.
        CreditCard card = creditCardRepository.save(CreditCard.builder()
                .user(testUser)
                .institution("Nu")
                .creditLimit(new BigDecimal("1000000"))
                .usedAmount(new BigDecimal("500000"))
                .cutOffDay(3)
                .paymentDueDay(23)
                .build());

        mockMvc.perform(patch("/api/v1/cards/" + card.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                {
                    "creditLimit": 200000
                }
            """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid Credit Limit"))
                .andExpect(jsonPath("$.message")
                        .value("Credit limit cannot be less than the amount already used. "
                                + "Requested: 200000. Currently used: 500000"));
    }

    @Test
    void dadoOtroUsuario_cuandoAccederTarjetaAjena_entoncesRetorna404() throws Exception {
        User otherUser = userRepository.save(User.builder()
                .email("other@finsight.dev")
                .name("Other User")
                .provider("google")
                .providerId("google-other-" + System.nanoTime())
                .build());

        CreditCard otherCard = creditCardRepository.save(CreditCard.builder()
                .user(otherUser)
                .institution("Bancolombia")
                .creditLimit(new BigDecimal("5000000"))
                .usedAmount(BigDecimal.ZERO)
                .cutOffDay(15)
                .paymentDueDay(25)
                .build());

        mockMvc.perform(get("/api/v1/cards/" + otherCard.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }
}