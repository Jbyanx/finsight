package dev.jbyanx.finsight.domain.telegram.bot;

import dev.jbyanx.finsight.domain.ai.service.AiService;
import dev.jbyanx.finsight.domain.telegram.bot.command.CommandDispatcher;
import dev.jbyanx.finsight.domain.telegram.service.TelegramLinkService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageRouterTest {

    @Mock private CommandDispatcher commandDispatcher;
    @Mock private TelegramLinkService telegramLinkService;
    @Mock private AiService aiService;

    @InjectMocks
    private MessageRouter router;

    private static final long CHAT_ID = 123L;

    // ---------- comandos ----------

    @Test
    void route_CommandText_GoesToDispatcher() {
        when(commandDispatcher.dispatch(CHAT_ID, "/help")).thenReturn("ayuda");

        BotReply reply = router.route(CHAT_ID, "/help");

        assertEquals("ayuda", reply.text());
        assertFalse(reply.withConfirmButtons());
        verify(aiService, never()).chat(anyString(), anyString(), anyString());
    }

    // ---------- lenguaje natural ----------

    @Test
    void route_NaturalLanguage_Linked_GoesToAi() {
        when(telegramLinkService.resolveUserId(CHAT_ID)).thenReturn("user-1");
        when(aiService.chat("123", "user-1", "¿cuánto tengo?"))
                .thenReturn(BotReply.text("Tenés $110.000"));

        BotReply reply = router.route(CHAT_ID, "¿cuánto tengo?");

        assertEquals("Tenés $110.000", reply.text());
        verify(aiService).chat("123", "user-1", "¿cuánto tengo?");
    }

    @Test
    void route_NaturalLanguage_NotLinked_AsksToLink() {
        when(telegramLinkService.resolveUserId(CHAT_ID)).thenReturn(null);

        BotReply reply = router.route(CHAT_ID, "¿cuánto tengo?");

        assertTrue(reply.text().contains("/start"));
        verify(aiService, never()).chat(anyString(), anyString(), anyString());
    }

    @Test
    void route_AiThrows_ReturnsFriendlyError() {
        when(telegramLinkService.resolveUserId(CHAT_ID)).thenReturn("user-1");
        when(aiService.chat(eq("123"), eq("user-1"), anyString()))
                .thenThrow(new RuntimeException("429 quota"));

        BotReply reply = router.route(CHAT_ID, "¿cuánto tengo?");

        assertTrue(reply.text().toLowerCase().contains("problemas"));
    }

    // ---------- callbacks ----------

    @Test
    void routeCallback_Linked_DelegatesToAi() {
        when(telegramLinkService.resolveUserId(CHAT_ID)).thenReturn("user-1");
        when(aiService.handleConfirmation("123", "user-1", "confirm"))
                .thenReturn(BotReply.text("Pago registrado."));

        BotReply reply = router.routeCallback(CHAT_ID, "confirm");

        assertEquals("Pago registrado.", reply.text());
    }

    @Test
    void routeCallback_NotLinked_AsksToLink() {
        when(telegramLinkService.resolveUserId(CHAT_ID)).thenReturn(null);

        BotReply reply = router.routeCallback(CHAT_ID, "confirm");

        assertTrue(reply.text().toLowerCase().contains("vinculada"));
        verify(aiService, never()).handleConfirmation(anyString(), anyString(), anyString());
    }

    // ---------- vacíos ----------

    @Test
    void route_BlankText_ReturnsHint() {
        BotReply reply = router.route(CHAT_ID, "   ");
        assertTrue(reply.text().contains("/help"));
        verifyNoInteractions(commandDispatcher, aiService);
    }
}