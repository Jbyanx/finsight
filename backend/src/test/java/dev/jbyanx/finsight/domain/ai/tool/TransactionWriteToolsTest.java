package dev.jbyanx.finsight.domain.ai.tool;

import dev.jbyanx.finsight.domain.ai.memory.Conversation;
import dev.jbyanx.finsight.domain.ai.memory.ConversationService;
import dev.jbyanx.finsight.domain.ai.pending.PendingAction;
import dev.jbyanx.finsight.domain.card.dto.CreditCardResponse;
import dev.jbyanx.finsight.domain.card.service.CreditCardService;
import dev.jbyanx.finsight.domain.cash.dto.CashResponse;
import dev.jbyanx.finsight.domain.cash.service.CashService;
import dev.jbyanx.finsight.domain.savings.dto.PocketResponse;
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

    private CreditCardResponse nu(String limit, String used) {
        return CreditCardResponse.builder()
                .id("card-1").nickname("Nu").institution("Nu")
                .creditLimit(new BigDecimal(limit))
                .usedAmount(new BigDecimal(used))
                .build();
    }

    /** Nequi: $1.000.000 total, $300.000 son de Papá, $200.000 en meta propia. */
    private SavingsAccountResponse nequi() {
        PocketResponse papa = PocketResponse.builder()
                .id("pk-papa").name("Papá")
                .balance(new BigDecimal("300000"))
                .shared(true).sharedOwnerName("Papá")
                .build();
        PocketResponse viaje = PocketResponse.builder()
                .id("pk-viaje").name("Viaje")
                .balance(new BigDecimal("200000"))
                .shared(false)
                .build();
        return SavingsAccountResponse.builder()
                .id("acc-1").nickname("Nequi").institution("Nequi")
                .balance(new BigDecimal("1000000"))
                .sharedTotal(new BigDecimal("300000"))
                .ownBalance(new BigDecimal("700000"))
                .availableBalance(new BigDecimal("500000"))
                .pockets(List.of(papa, viaje))
                .build();
    }

    // ---------- Contexto ----------

    @Test
    void noUserId_DoesNotStashPending() {
        String result = tools.previewRegisterTransaction(
                "mercado", new BigDecimal("50000"), "efectivo",
                null, null, null, new ToolContext(Map.of()));

        assertTrue(result.toLowerCase().contains("no se pudo identificar"));
        verify(conversationService, never()).setPending(any(), any());
    }

    // ---------- Efectivo ----------

    @Test
    void cash_WithinBalance_StashesFormattedPreview() {
        when(cashService.getByUser("user-1")).thenReturn(
                CashResponse.builder().balance(new BigDecimal("200000")).build());
        when(conversationService.getOrCreate("chat-1", "user-1")).thenReturn(new Conversation());

        String result = tools.previewRegisterTransaction(
                "mercado", new BigDecimal("50000"), "efectivo", "Comida", null, null,
                ctx("user-1", "chat-1"));

        assertTrue(result.contains("$150.000"));
        verify(conversationService).setPending(any(Conversation.class), any(PendingAction.class));
    }

    @Test
    void cash_OverBalance_DoesNotStash() {
        when(cashService.getByUser("user-1")).thenReturn(
                CashResponse.builder().balance(new BigDecimal("30000")).build());

        String result = tools.previewRegisterTransaction(
                "mercado", new BigDecimal("50000"), "efectivo", null, null, null,
                ctx("user-1", "chat-1"));

        assertTrue(result.toLowerCase().contains("supera"));
        verify(conversationService, never()).setPending(any(), any());
    }

    @Test
    void cash_WithMoneyOwner_IsRejected() {
        // El dinero de terceros vive en bolsillos, no en efectivo.
        String result = tools.previewRegisterTransaction(
                "mercado", new BigDecimal("50000"), "efectivo", null, null, "papá",
                ctx("user-1", "chat-1"));

        assertTrue(result.toLowerCase().contains("bolsillo"));
        verify(conversationService, never()).setPending(any(), any());
    }

    // ---------- Tarjeta ----------

    @Test
    void card_WithinLimit_ShowsProjectedUtilization() {
        when(creditCardService.getAllByUser("user-1")).thenReturn(List.of(nu("400000", "0")));
        when(conversationService.getOrCreate("chat-1", "user-1")).thenReturn(new Conversation());

        String result = tools.previewRegisterTransaction(
                "tenis", new BigDecimal("200000"), "Nu", null, null, null,
                ctx("user-1", "chat-1"));

        assertTrue(result.contains("50.00"));
        assertTrue(result.contains("$200.000"));
        verify(conversationService).setPending(any(Conversation.class), any(PendingAction.class));
    }

    @Test
    void card_Installments_MentionedInPreview() {
        when(creditCardService.getAllByUser("user-1")).thenReturn(List.of(nu("1000000", "0")));
        when(conversationService.getOrCreate("chat-1", "user-1")).thenReturn(new Conversation());

        String result = tools.previewRegisterTransaction(
                "tenis", new BigDecimal("300000"), "Nu", null, 6, null,
                ctx("user-1", "chat-1"));

        assertTrue(result.contains("6 cuotas"));
    }

    @Test
    void card_OverLimit_DoesNotStash() {
        when(creditCardService.getAllByUser("user-1")).thenReturn(List.of(nu("100000", "90000")));

        String result = tools.previewRegisterTransaction(
                "mercado", new BigDecimal("50000"), "Nu", null, null, null,
                ctx("user-1", "chat-1"));

        assertTrue(result.toLowerCase().contains("cupo"));
        verify(conversationService, never()).setPending(any(), any());
    }

    @Test
    void card_WithMoneyOwner_IsRejected() {
        when(creditCardService.getAllByUser("user-1")).thenReturn(List.of(nu("400000", "0")));

        String result = tools.previewRegisterTransaction(
                "mercado", new BigDecimal("50000"), "Nu", null, null, "papá",
                ctx("user-1", "chat-1"));

        assertTrue(result.toLowerCase().contains("tarjeta"));
        verify(conversationService, never()).setPending(any(), any());
    }

    // ---------- Cuenta: dinero propio ----------

    @Test
    void savings_OwnMoney_ValidatesAgainstAvailableNotBalance() {
        when(creditCardService.getAllByUser("user-1")).thenReturn(List.of());
        when(savingsAccountService.getAllByUser("user-1")).thenReturn(List.of(nequi()));
        when(conversationService.getOrCreate("chat-1", "user-1")).thenReturn(new Conversation());

        String result = tools.previewRegisterTransaction(
                "retiro", new BigDecimal("100000"), "nequi", null, null, null,
                ctx("user-1", "chat-1"));

        // libre 500.000 - 100.000 = 400.000
        assertTrue(result.contains("$400.000"));
        verify(conversationService).setPending(any(Conversation.class), any(PendingAction.class));
    }

    @Test
    void savings_OwnMoney_OverAvailable_DoesNotStashEvenIfBalanceCovers() {
        // $600.000 < balance ($1.000.000) pero > libre ($500.000): debe frenar.
        // Esto es lo que impide gastar sin querer la plata de Papá.
        when(creditCardService.getAllByUser("user-1")).thenReturn(List.of());
        when(savingsAccountService.getAllByUser("user-1")).thenReturn(List.of(nequi()));

        String result = tools.previewRegisterTransaction(
                "compra", new BigDecimal("600000"), "nequi", null, null, null,
                ctx("user-1", "chat-1"));

        assertTrue(result.toLowerCase().contains("dinero libre"));
        verify(conversationService, never()).setPending(any(), any());
    }

    // ---------- Cuenta: dinero de terceros ----------

    @Test
    void savings_ThirdPartyMoney_WarnsItIsNotYours() {
        when(creditCardService.getAllByUser("user-1")).thenReturn(List.of());
        when(savingsAccountService.getAllByUser("user-1")).thenReturn(List.of(nequi()));
        when(conversationService.getOrCreate("chat-1", "user-1")).thenReturn(new Conversation());

        String result = tools.previewRegisterTransaction(
                "Factura Tigo", new BigDecimal("100000"), "nequi", null, null, "papá",
                ctx("user-1", "chat-1"));

        assertTrue(result.contains("Papá"));
        assertTrue(result.toLowerCase().contains("no es tuya"));
        assertTrue(result.contains("$200.000")); // bolsillo 300.000 - 100.000
        verify(conversationService).setPending(any(Conversation.class), any(PendingAction.class));
    }

    @Test
    void savings_ThirdPartyMoney_OverPocketBalance_DoesNotStash() {
        when(creditCardService.getAllByUser("user-1")).thenReturn(List.of());
        when(savingsAccountService.getAllByUser("user-1")).thenReturn(List.of(nequi()));

        String result = tools.previewRegisterTransaction(
                "Factura", new BigDecimal("400000"), "nequi", null, null, "papá",
                ctx("user-1", "chat-1"));

        assertTrue(result.toLowerCase().contains("supera"));
        verify(conversationService, never()).setPending(any(), any());
    }

    @Test
    void savings_UnknownOwner_ListsAvailableOwners() {
        when(creditCardService.getAllByUser("user-1")).thenReturn(List.of());
        when(savingsAccountService.getAllByUser("user-1")).thenReturn(List.of(nequi()));

        String result = tools.previewRegisterTransaction(
                "Factura", new BigDecimal("50000"), "nequi", null, null, "abuela",
                ctx("user-1", "chat-1"));

        assertTrue(result.contains("Papá"));  // lista los dueños que sí existen
        verify(conversationService, never()).setPending(any(), any());
    }

    // ---------- Origen inexistente ----------

    @Test
    void unknownOrigin_DoesNotStash() {
        when(creditCardService.getAllByUser("user-1")).thenReturn(List.of(nu("400000", "0")));
        when(savingsAccountService.getAllByUser("user-1")).thenReturn(List.of());

        String result = tools.previewRegisterTransaction(
                "mercado", new BigDecimal("50000"), "Falabella", null, null, null,
                ctx("user-1", "chat-1"));

        assertTrue(result.toLowerCase().contains("no encontré"));
        verify(conversationService, never()).setPending(any(), any());
    }
}