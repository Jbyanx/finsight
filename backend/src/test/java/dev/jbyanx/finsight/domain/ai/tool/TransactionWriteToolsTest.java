package dev.jbyanx.finsight.domain.ai.tool;

import dev.jbyanx.finsight.domain.ai.memory.Conversation;
import dev.jbyanx.finsight.domain.ai.memory.ConversationService;
import dev.jbyanx.finsight.domain.ai.pending.PendingAction;
import dev.jbyanx.finsight.domain.card.dto.CreditCardResponse;
import dev.jbyanx.finsight.domain.card.service.CreditCardService;
import dev.jbyanx.finsight.domain.cash.dto.CashResponse;
import dev.jbyanx.finsight.domain.cash.service.CashService;
import dev.jbyanx.finsight.domain.savings.dto.SavingsAccountResponse;
import dev.jbyanx.finsight.domain.savings.service.SavingsAccountService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionWriteToolsTest {

    @Mock private CreditCardService creditCardService;
    @Mock private SavingsAccountService savingsAccountService;
    @Mock private CashService cashService;
    @Mock private ConversationService conversationService;

    @InjectMocks private TransactionWriteTools tools;

    private ToolContext ctx(String userId, String chatId) {
        return new ToolContext(Map.of(
                ToolContextKeys.USER_ID, userId,
                ToolContextKeys.CHAT_ID, chatId));
    }

    @Test
    void previewRegisterTransaction_NoUserId_DoesNotStashPending() {
        ToolContext ctx = new ToolContext(Map.of());

        String result = tools.previewRegisterTransaction(
                "mercado", new BigDecimal("50000"), "efectivo", null, null, ctx);

        assertTrue(result.toLowerCase().contains("no se pudo identificar"));
        verify(conversationService, never()).setPending(any(), any());
    }

    @Test
    void previewRegisterTransaction_Cash_WithinBalance_StashesPendingWithFormattedBalance() {
        when(cashService.getByUser("user-1")).thenReturn(
                CashResponse.builder().balance(new BigDecimal("200000")).build());
        when(conversationService.getOrCreate("chat-1", "user-1"))
                .thenReturn(new Conversation());

        String result = tools.previewRegisterTransaction(
                "mercado", new BigDecimal("50000"), "efectivo", "Comida", null,
                ctx("user-1", "chat-1"));

        assertTrue(result.contains("$150.000"));
        verify(conversationService).setPending(any(Conversation.class), any(PendingAction.class));
    }

    @Test
    void previewRegisterTransaction_Cash_OverBalance_DoesNotStash() {
        when(cashService.getByUser("user-1")).thenReturn(
                CashResponse.builder().balance(new BigDecimal("30000")).build());

        String result = tools.previewRegisterTransaction(
                "mercado", new BigDecimal("50000"), "efectivo", null, null,
                ctx("user-1", "chat-1"));

        assertTrue(result.toLowerCase().contains("supera"));
        verify(conversationService, never()).setPending(any(), any());
    }

    @Test
    void previewRegisterTransaction_Card_WithinLimit_ShowsProjectedUtilization() {
        when(creditCardService.getAllByUser("user-1")).thenReturn(List.of(
                CreditCardResponse.builder()
                        .id("card-1")
                        .nickname("Nu")
                        .institution("Nu")
                        .creditLimit(new BigDecimal("400000"))
                        .usedAmount(BigDecimal.ZERO)
                        .build()));
        when(conversationService.getOrCreate("chat-1", "user-1"))
                .thenReturn(new Conversation());

        String result = tools.previewRegisterTransaction(
                "tenis", new BigDecimal("200000"), "Nu", null, null,
                ctx("user-1", "chat-1"));

        assertTrue(result.contains("50.00"));      // utilización proyectada
        assertTrue(result.contains("$200.000"));   // monto formateado
        verify(conversationService).setPending(any(Conversation.class), any(PendingAction.class));
    }

    @Test
    void previewRegisterTransaction_Card_Installments_MentionedInPreview() {
        when(creditCardService.getAllByUser("user-1")).thenReturn(List.of(
                CreditCardResponse.builder()
                        .id("card-1").nickname("Nu").institution("Nu")
                        .creditLimit(new BigDecimal("1000000"))
                        .usedAmount(BigDecimal.ZERO).build()));
        when(conversationService.getOrCreate("chat-1", "user-1"))
                .thenReturn(new Conversation());

        String result = tools.previewRegisterTransaction(
                "tenis", new BigDecimal("300000"), "Nu", null, 6,
                ctx("user-1", "chat-1"));

        assertTrue(result.contains("6 cuotas"));
    }

    @Test
    void previewRegisterTransaction_Card_OverLimit_DoesNotStash() {
        when(creditCardService.getAllByUser("user-1")).thenReturn(List.of(
                CreditCardResponse.builder()
                        .id("card-1")
                        .nickname("Nu")
                        .institution("Nu")
                        .creditLimit(new BigDecimal("100000"))
                        .usedAmount(new BigDecimal("90000"))
                        .build()));

        String result = tools.previewRegisterTransaction(
                "mercado", new BigDecimal("50000"), "Nu", null, null,
                ctx("user-1", "chat-1"));

        assertTrue(result.toLowerCase().contains("cupo"));
        verify(conversationService, never()).setPending(any(), any());
    }

    @Test
    void previewRegisterTransaction_Savings_WithinBalance_StashesPending() {
        when(creditCardService.getAllByUser("user-1")).thenReturn(List.of());
        when(savingsAccountService.getAllByUser("user-1")).thenReturn(List.of(
                SavingsAccountResponse.builder()
                        .id("acc-1")
                        .nickname("Bancolombia")
                        .institution("Bancolombia")
                        .balance(new BigDecimal("500000"))
                        .build()));
        when(conversationService.getOrCreate("chat-1", "user-1"))
                .thenReturn(new Conversation());

        String result = tools.previewRegisterTransaction(
                "retiro", new BigDecimal("100000"), "bancolombia", null, null,
                ctx("user-1", "chat-1"));

        assertTrue(result.contains("$400.000"));
        verify(conversationService).setPending(any(Conversation.class), any(PendingAction.class));
    }

    @Test
    void previewRegisterTransaction_UnknownOrigin_DoesNotStash() {
        when(creditCardService.getAllByUser("user-1")).thenReturn(List.of(
                CreditCardResponse.builder()
                        .id("card-1").nickname("Nu").institution("Nu")
                        .creditLimit(new BigDecimal("400000"))
                        .usedAmount(BigDecimal.ZERO).build()));
        when(savingsAccountService.getAllByUser("user-1")).thenReturn(List.of());

        String result = tools.previewRegisterTransaction(
                "mercado", new BigDecimal("50000"), "Falabella", null, null,
                ctx("user-1", "chat-1"));

        assertTrue(result.toLowerCase().contains("no encontré"));
        verify(conversationService, never()).setPending(any(), any());
    }
}