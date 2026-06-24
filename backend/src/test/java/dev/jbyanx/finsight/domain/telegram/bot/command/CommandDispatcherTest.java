package dev.jbyanx.finsight.domain.telegram.bot.command;

import dev.jbyanx.finsight.domain.telegram.service.TelegramLinkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommandDispatcherTest {

    @Mock
    private TelegramLinkService telegramLinkService;

    private CommandDispatcher dispatcher;

    // Handler público (no requiere vínculo), devuelve lo que recibe
    private static CommandHandler publicHandler(String cmd) {
        return new CommandHandler() {
            @Override public String command() { return cmd; }
            @Override public boolean requiresLink() { return false; }
            @Override public String handle(long chatId, String args) {
                return cmd + "|" + chatId + "|" + args;
            }
        };
    }

    // Handler protegido (requiere vínculo)
    private static CommandHandler protectedHandler(String cmd) {
        return new CommandHandler() {
            @Override public String command() { return cmd; }
            // requiresLink() usa el default true
            @Override public String handle(long chatId, String args) {
                return cmd + "|" + chatId + "|" + args;
            }
        };
    }

    @BeforeEach
    void setUp() {
        dispatcher = new CommandDispatcher(
                List.of(publicHandler("/start"),
                        publicHandler("/help"),
                        protectedHandler("/saldo")),
                telegramLinkService);
    }

    // ---------- comandos públicos (no requieren vínculo) ----------

    @Test
    void dispatch_PublicCommandNoArgs_RoutesWithoutLinkCheck() {
        assertEquals("/help|123|", dispatcher.dispatch(123L, "/help"));
        verify(telegramLinkService, never()).resolveUserId(anyLong());
    }

    @Test
    void dispatch_PublicCommandWithArgs_ParsesArgs() {
        assertEquals("/start|123|ABC234", dispatcher.dispatch(123L, "/start ABC234"));
    }

    @Test
    void dispatch_CommandIsCaseInsensitive() {
        assertEquals("/start|123|ABC234", dispatcher.dispatch(123L, "/START ABC234"));
    }

    @Test
    void dispatch_ExtraWhitespace_IsTrimmed() {
        assertEquals("/start|123|ABC234", dispatcher.dispatch(123L, "  /start    ABC234  "));
    }

    // ---------- bloqueo de vinculación ----------

    @Test
    void dispatch_ProtectedCommand_NotLinked_IsBlocked() {
        when(telegramLinkService.resolveUserId(123L)).thenReturn(null);

        String result = dispatcher.dispatch(123L, "/saldo");

        assertTrue(result.contains("/start"));   // mensaje guía de vinculación
        assertFalse(result.contains("/saldo|")); // el handler NO se ejecutó
    }

    @Test
    void dispatch_ProtectedCommand_Linked_IsRouted() {
        when(telegramLinkService.resolveUserId(123L)).thenReturn("user-1");

        assertEquals("/saldo|123|", dispatcher.dispatch(123L, "/saldo"));
    }

    // ---------- desconocidos / vacíos ----------

    @Test
    void dispatch_UnknownCommand_ReturnsHelpHint() {
        assertTrue(dispatcher.dispatch(123L, "/banana").contains("/help"));
    }

    @Test
    void dispatch_NullText_ReturnsHelpHint() {
        assertTrue(dispatcher.dispatch(123L, null).contains("/help"));
    }

    @Test
    void dispatch_BlankText_ReturnsHelpHint() {
        assertTrue(dispatcher.dispatch(123L, "   ").contains("/help"));
    }
}