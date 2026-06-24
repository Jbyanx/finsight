package dev.jbyanx.finsight.domain.telegram.bot.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CommandDispatcherTest {

    private CommandDispatcher dispatcher;

    // Handlers de prueba que devuelven lo que reciben, para verificar parseo
    private static CommandHandler handler(String cmd) {
        return new CommandHandler() {
            @Override public String command() { return cmd; }
            @Override public String handle(long chatId, String args) {
                return cmd + "|" + chatId + "|" + args;
            }
        };
    }

    @BeforeEach
    void setUp() {
        dispatcher = new CommandDispatcher(List.of(handler("/start"), handler("/help")));
    }

    @Test
    void dispatch_KnownCommandNoArgs_RoutesWithEmptyArgs() {
        assertEquals("/help|123|", dispatcher.dispatch(123L, "/help"));
    }

    @Test
    void dispatch_KnownCommandWithArgs_ParsesArgs() {
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

    @Test
    void dispatch_UnknownCommand_ReturnsHelpHint() {
        String result = dispatcher.dispatch(123L, "/banana");
        assertTrue(result.contains("/help"));
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