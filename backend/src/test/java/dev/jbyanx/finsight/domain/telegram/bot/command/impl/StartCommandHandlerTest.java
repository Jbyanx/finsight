package dev.jbyanx.finsight.domain.telegram.bot.command.impl;

import dev.jbyanx.finsight.domain.telegram.dto.TelegramLinkResponse;
import dev.jbyanx.finsight.domain.telegram.service.TelegramLinkService;
import dev.jbyanx.finsight.shared.exception.UnauthorizedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StartCommandHandlerTest {

    @Mock
    private TelegramLinkService telegramLinkService;

    @InjectMocks
    private StartCommandHandler handler;

    private static final long CHAT_ID = 123L;

    @BeforeEach
    void setUp() {
        // nada extra
    }

    @Test
    void handle_WithValidCode_LinksAndGreetsByName() {
        when(telegramLinkService.linkAccount("ABC234", CHAT_ID))
                .thenReturn(TelegramLinkResponse.builder()
                        .linked(true).userName("Jabes").build());

        String result = handler.handle(CHAT_ID, "ABC234");

        assertTrue(result.contains("Jabes"));
        verify(telegramLinkService).linkAccount("ABC234", CHAT_ID);
    }

    @Test
    void handle_NoCode_ReturnsWelcomeWithoutLinking() {
        String result = handler.handle(CHAT_ID, "");

        assertTrue(result.contains("/start"));
        verify(telegramLinkService, never()).linkAccount(anyString(), anyLong());
    }

    @Test
    void handle_InvalidCode_ReturnsFriendlyError() {
        when(telegramLinkService.linkAccount("BADXXX", CHAT_ID))
                .thenThrow(new UnauthorizedException("Invalid or expired code"));

        String result = handler.handle(CHAT_ID, "BADXXX");

        assertTrue(result.toLowerCase().contains("no pude vincular"));
    }

    @Test
    void handle_TrimsCodeWhitespace() {
        when(telegramLinkService.linkAccount("ABC234", CHAT_ID))
                .thenReturn(TelegramLinkResponse.builder()
                        .linked(true).userName("Jabes").build());

        handler.handle(CHAT_ID, "  ABC234  ");

        verify(telegramLinkService).linkAccount("ABC234", CHAT_ID);
    }

    @Test
    void requiresLink_IsFalse() {
        assertFalse(handler.requiresLink());
    }
}