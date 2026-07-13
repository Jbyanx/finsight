package dev.jbyanx.finsight.domain.telegram.bot.command.impl;

import dev.jbyanx.finsight.domain.telegram.service.TelegramLinkService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnlinkCommandHandlerTest {

    @Mock private TelegramLinkService telegramLinkService;
    @InjectMocks private UnlinkCommandHandler handler;

    @Test
    void command_IsUnlink() {
        assertEquals("/unlink", handler.command());
    }

    @Test
    void requiresLink_IsTrue() {
        assertTrue(handler.requiresLink());
    }

    @Test
    void handle_WhenLinked_ConfirmsAndExplainsHowToReconnect() {
        when(telegramLinkService.unlinkByChatId(123L)).thenReturn(true);

        String result = handler.handle(123L, null);

        assertTrue(result.toLowerCase().contains("desvinculé"));
        assertTrue(result.contains("/start"));
    }

    @Test
    void handle_WhenNotLinked_SaysSoWithoutError() {
        when(telegramLinkService.unlinkByChatId(123L)).thenReturn(false);

        String result = handler.handle(123L, null);

        assertTrue(result.toLowerCase().contains("no estaba vinculada"));
    }
}