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

        assertEquals("ayuda", router.route(CHAT_ID, "/help"));
        verify(commandDispatcher).dispatch(CHAT_ID, "/help");
        verify(aiService, never()).chat(anyString(), anyString());
    }

    // ---------- lenguaje natural ----------

    @Test
    void route_NaturalLanguage_Linked_GoesToAi() {
        when(telegramLinkService.resolveUserId(CHAT_ID)).thenReturn("user-1");
        when(aiService.chat("user-1", "¿cuánto tengo?")).thenReturn("Tenés $110.000");

        assertEquals("Tenés $110.000", router.route(CHAT_ID, "¿cuánto tengo?"));
        verify(aiService).chat("user-1", "¿cuánto tengo?");
        verify(commandDispatcher, never()).dispatch(anyLong(), anyString());
    }

    @Test
    void route_NaturalLanguage_NotLinked_AsksToLink() {
        when(telegramLinkService.resolveUserId(CHAT_ID)).thenReturn(null);

        String result = router.route(CHAT_ID, "¿cuánto tengo?");

        assertTrue(result.contains("/start"));
        verify(aiService, never()).chat(anyString(), anyString());
    }

    @Test
    void route_AiThrows_ReturnsFriendlyError() {
        when(telegramLinkService.resolveUserId(CHAT_ID)).thenReturn("user-1");
        when(aiService.chat(eq("user-1"), anyString()))
                .thenThrow(new RuntimeException("429 quota"));

        String result = router.route(CHAT_ID, "¿cuánto tengo?");

        assertTrue(result.toLowerCase().contains("problemas"));
    }

    // ---------- vacíos ----------

    @Test
    void route_BlankText_ReturnsHint() {
        assertTrue(router.route(CHAT_ID, "   ").contains("/help"));
        verifyNoInteractions(commandDispatcher, aiService);
    }

    @Test
    void route_NullText_ReturnsHint() {
        assertTrue(router.route(CHAT_ID, null).contains("/help"));
    }
}